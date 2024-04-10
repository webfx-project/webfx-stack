package dev.webfx.stack.cloud.image.impl.cloudinary;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.FormData;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.fetch.Method;
import dev.webfx.platform.file.File;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.cloud.image.impl.fetchbased.FetchBasedCloudImageService;
import dev.webfx.stack.hash.sha1.Sha1;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public class Cloudinary extends FetchBasedCloudImageService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public Cloudinary(String cloudName, String apiKey, String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    protected Headers createHeaders() {
        return super.createHeaders()
                .set("Authorization", "Basic " + Base64Coder.encode(apiKey + ":" + apiSecret));
    }

    public Future<Boolean> exists(String publicId) {
        return fetchAndConvertJsonObject(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/resources/search",
                Method.POST,
                new FetchOptions().setBody(AST.formatObject(AST.createObject().set("expression", "public_id=" + publicId), "json")),
                json -> {
                    ReadOnlyAstArray resources = json.getArray("resources");
                    return resources != null && !resources.isEmpty();
                }
        );
    }

    public Future<Map> upload(File file, String publicId, boolean overwrite) {
        return fetchAndConvertJsonObjectToMap(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                Method.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                            .append("public_id", publicId)
                            .append("overwrite", overwrite)
                        ).append("file", file, publicId)
                )
        );
    }

    public Future<Map> destroy(String publicId, boolean invalidate) {
        return fetchAndConvertJsonObjectToMap(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy",
                Method.POST,
                new FetchOptions().setBody(
                        signFormData(new FormData()
                                .append("public_id", publicId)
                                .append("invalidate", invalidate)
                        )
                )
        );
    }

    public String url(String source, int width, int height) {
        String url = "https://res.cloudinary.com/" + cloudName + "/image/upload/";
        if (width > 0)
            url += "w_" + width + "/";
        if (height > 0)
            url += "h_" + height + "/";
        url += source;
        return url;
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
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        Collection<String> params = new ArrayList<String>();
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
