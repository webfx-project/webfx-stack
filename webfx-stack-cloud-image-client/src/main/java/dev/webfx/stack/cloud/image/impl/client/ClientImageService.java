package dev.webfx.stack.cloud.image.impl.client;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.file.File;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.platform.util.http.HttpResponseStatus;
import dev.webfx.stack.cloud.image.CloudImageService;

/**
 * @author Bruno Salmon
 */
public class ClientImageService implements CloudImageService {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.rest";

    private String existsUrl;
    private String uploadUrl;
    private String deleteUrl;
    private String urlPattern;

    public ClientImageService() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            existsUrl = config.getString("existsUrl");
            uploadUrl = config.getString("uploadUrl");
            deleteUrl = config.getString("deleteUrl");
            String urlPatternUrl = config.getString("urlPatternUrl");
            Fetch.fetchText(urlPatternUrl, new FetchOptions()
                    .setMethod(HttpMethod.GET)
                    .setMode(CorsMode.NO_CORS)
            ).onSuccess(text -> urlPattern = text);
        });
    }

    public Future<Boolean> exists(String id) {
        return Fetch.fetch(existsUrl, new FetchOptions()
                .setMethod(HttpMethod.POST)
                .setMode(CorsMode.NO_CORS)
                .setBody(new FormData().append("id", id))
        ).compose(response -> {
            if (response.ok())
                return Future.succeededFuture(response.status() == HttpResponseStatus.OK_200); // OK_200 = exists, NO_CONTENT_204 = doesn't exist
            return Future.failedFuture(response.statusText());
        });
    }

    public Future<Void> upload(File file, String id, boolean overwrite) {
        return Fetch.fetch(uploadUrl, new FetchOptions()
                .setMethod(HttpMethod.POST)
                .setMode(CorsMode.NO_CORS)
                .setBody(new FormData()
                        .append("id", id)
                        .append("overwrite", overwrite)
                        .append("file", file, id)
                )
        ).map(response -> null);
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return Fetch.fetch(deleteUrl, new FetchOptions()
                .setMethod(HttpMethod.POST)
                .setMode(CorsMode.NO_CORS)
                .setBody(new FormData()
                    .append("id", id)
                    .append("invalidate", invalidate)
                )
        ).map(response -> null);
    }

    @Override
    public String urlPattern() {
        return urlPattern;
    }
}
