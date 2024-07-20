package dev.webfx.stack.cloud.image.impl.cloudinary;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.file.File;
import dev.webfx.platform.util.http.HttpHeaders;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.cloud.image.impl.fetchbased.FetchBasedCloudImageService;
import dev.webfx.stack.hash.sha1.Sha1;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public class Cloudinary extends FetchBasedCloudImageService {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.cloudinary";

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    public Cloudinary() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            cloudName = config.getString("cloudName");
            apiKey = config.getString("apiKey");
            apiSecret = config.getString("apiSecret");
        });
    }

    @Override
    protected Headers createHeaders() {
        return super.createHeaders()
                .set(HttpHeaders.AUTHORIZATION, HttpHeaders.basicAuth(apiKey, apiSecret));
    }

    public Future<Boolean> exists(String id) {
        return Fetch.fetch(url(id, -1, -1), new FetchOptions().setMethod(HttpMethod.HEAD))
                .map(Response::ok);
    }

    public Future<Void> upload(File file, String id, boolean overwrite) {
        return fetchJsonObject(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                HttpMethod.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                            .append("public_id", id)
                            .append("overwrite", overwrite)
                        ).append("file", file, id)
                )
        ).map(json -> null);
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return fetchJsonObject(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy",
                HttpMethod.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                                .append("public_id", id)
                                .append("invalidate", invalidate)
                        )
                )
        ).map(json -> null);
    }

    @Override
    public String urlPattern() {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/w_:width/h_:height/:source";
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
                params.add(param.getKey() + "=" + Collections.toString((Collection) param.getValue(), false, false));
            } /*else if (param.getValue() instanceof Object[]) {
                params.add(param.getKey() + "=" + StringUtils.join((Object[]) param.getValue(), ","));
            }*/ else {
                String stringValue = Strings.toString(param.getValue());
                if (!Strings.isBlank(stringValue)) {
                    params.add(param.getKey() + "=" + stringValue);
                }
            }
        }

        String to_sign = Collections.toString(params, "&", false, false);
        String hash = Sha1.hash(to_sign + apiSecret);
        return hash;
    }

}
