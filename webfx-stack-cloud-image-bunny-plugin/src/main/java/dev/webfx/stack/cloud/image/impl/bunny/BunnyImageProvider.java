package dev.webfx.stack.cloud.image.impl.bunny;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.http.HttpHeaders;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.jsonfetchapi.JsonFetchApiCloudImageProvider;
import dev.webfx.stack.hash.sha1.Sha1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class BunnyImageProvider extends JsonFetchApiCloudImageProvider {

    private static final boolean LOG_JSON_REPLY = true;
    private static final String CONFIG_PATH = "webfx.stack.cloud.image.bunny";

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private final Future<Void> configFuture;

    public BunnyImageProvider() {
        Promise<Void> configPromise = Promise.promise();
        configFuture = configPromise.future();
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            cloudName = config.getString("cloudName");
            apiKey = config.getString("apiKey");
            apiSecret = config.getString("apiSecret");
            configPromise.complete();
        });
    }

    @Override
    public Future<Void> readyFuture() {
        return configFuture;
    }


    // Public API (authentication not required)

    public Future<Boolean> exists(String id) {
        return Fetch.fetch(url(id, -1, -1), new FetchOptions().setMethod(HttpMethod.HEAD))
            .map(Response::ok);
    }

    @Override
    public String urlPattern() {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/w_:width/h_:height/:source";
    }

    @Override
    public String url(String source, int width, int height) {
        String url = super.url(source, width, height);
        if (width < 0)
            url = url.replaceFirst("w_:-1", "");
        if (height < 0)
            url = url.replaceFirst("h_:-1", "");
        return url;
    }

    // Private API (authentication required)

    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        return fetchJsonApiObject(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                HttpMethod.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                            .append("public_id", id)
                            .append("overwrite", overwrite)
                            .append("invalidate", true) // Otherwise the new image might not be displayed immediately after upload
                        ).append("file", blob, id)
                )
        ).map(json -> logJsonReply("upload", json));
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return fetchJsonApiObject(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy",
                HttpMethod.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                                .append("public_id", id)
                                .append("invalidate", invalidate)
                        )
                )
        ).map(json -> logJsonReply("delete", json));
    }

    @Override
    protected Headers createJsonApiHeaders() {
        return super.createJsonApiHeaders()
            .set(HttpHeaders.AUTHORIZATION, HttpHeaders.basicAuth(apiKey, apiSecret));
    }

    private static Void logJsonReply(String operation, ReadOnlyAstObject jsonReply) {
        if (LOG_JSON_REPLY) {
            Console.log("[CLOUDINARY] - " + operation + " - json reply = " + AST.formatObject(jsonReply, "json"));
        }
        return null;
    }

    private FormData signFormData(FormData formData) {
        if (!formData.has("timestamp"))
            formData.set("timestamp", timestamp());
        formData.set("signature", apiSignRequest(formData, apiSecret));
        formData.set("api_key", apiKey);
        return formData;
    }

    private static String timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000L);
    }

    private static String apiSignRequest(FormData paramsToSign, String apiSecret) {
        List<Map.Entry<String, Object>> entries = new ArrayList<>(paramsToSign.entries());
        entries.sort(Map.Entry.comparingByKey());
        Collection<String> params = new ArrayList<>();
        for (Map.Entry<String, Object> param : entries) {
            if (param.getValue() instanceof Collection) {
                params.add(param.getKey() + "=" + Collections.toStringCommaSeparated((Collection) param.getValue()));
            } /*else if (param.getValue() instanceof Object[]) {
                params.add(param.getKey() + "=" + StringUtils.join((Object[]) param.getValue(), ","));
            }*/ else {
                String stringValue = Strings.toString(param.getValue());
                if (!Strings.isBlank(stringValue)) {
                    params.add(param.getKey() + "=" + stringValue);
                }
            }
        }

        String to_sign = Collections.toStringAmpersandSeparated(params);
        return Sha1.hash(to_sign + apiSecret);
    }

}
