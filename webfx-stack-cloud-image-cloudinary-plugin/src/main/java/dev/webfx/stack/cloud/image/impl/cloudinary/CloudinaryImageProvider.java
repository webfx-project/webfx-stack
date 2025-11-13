package dev.webfx.stack.cloud.image.impl.cloudinary;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.http.HttpHeaders;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.fetch.JsonFetchApiCloudImageProvider;
import dev.webfx.stack.hash.sha1.Sha1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class CloudinaryImageProvider extends JsonFetchApiCloudImageProvider {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.cloudinary";
    private static final boolean LOG_JSON_REPLY = true;

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    public CloudinaryImageProvider() {
        super(CONFIG_PATH);
    }

    @Override
    protected void onConfigLoaded(Config config) {
        cloudName = config.getString("cloudName");
        apiKey = config.getString("apiKey");
        apiSecret = config.getString("apiSecret");
        setUrlPattern("https://res.cloudinary.com/" + cloudName + "/image/upload(/w_:width)(/h_:height)/:source?t=:timestamp");
    }

    // Private API (authentication required)

    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        return callCloudinaryApi("upload", signFormData(new FormData()
            .append("public_id", id)
            .append("overwrite", overwrite)
            .append("invalidate", true) // Otherwise the new image might not be displayed immediately after upload
        ).append("file", blob, id));
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return callCloudinaryApi("destroy", signFormData(new FormData()
                .append("public_id", id)
                .append("invalidate", invalidate)
            )
        );
    }

    private Future<Void> callCloudinaryApi(String operation, FormData body) {
        return fetchJsonApiObject("https://api.cloudinary.com/v1_1/" + cloudName + "/image/" + operation, body, HttpMethod.POST)
            .compose(json -> {
                if (LOG_JSON_REPLY)
                    Console.log("[CLOUDINARY] - " + operation + " - json reply = " + AST.formatObject(json, "json"));
                ReadOnlyAstObject error = json.getObject("error");
                if (error != null && error.has("message")) {
                    return Future.failedFuture(error.getString("message"));
                }
                return Future.succeededFuture(json);
            }).mapEmpty();
    }

    @Override
    protected Headers createApiHeaders(String contentType) {
        return super.createApiHeaders(contentType)
            .set(HttpHeaders.AUTHORIZATION, HttpHeaders.basicAuth(apiKey, apiSecret));
    }

    private FormData signFormData(FormData formData) {
        if (!formData.has("timestamp"))
            formData.set("timestamp", timestamp());
        formData.set("signature", apiSignRequest(formData, apiSecret));
        formData.set("api_key", apiKey);
        return formData;
    }

    private static String timestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000L);
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
