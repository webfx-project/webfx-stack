package dev.webfx.stack.cloud.image.impl.rest.client;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.CorsMode;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.FormData;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.platform.util.http.HttpResponseStatus;
import dev.webfx.stack.cloud.image.spi.CloudImageProvider;

/**
 * @author Bruno Salmon
 */
public class RestClientImageProvider implements CloudImageProvider {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.rest";

    private String existsUrl;
    private String uploadUrl;
    private String deleteUrl;
    private String urlPattern;
    private final Future<Void> urlPatternFuture;

    public RestClientImageProvider() {
        Promise<Void> urlPatternPromise = Promise.promise();
        urlPatternFuture = urlPatternPromise.future();
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            existsUrl = config.getString("existsUrl");
            uploadUrl = config.getString("uploadUrl");
            deleteUrl = config.getString("deleteUrl");
            String urlPatternUrl = config.getString("urlPatternUrl");
            Fetch.fetchText(urlPatternUrl, new FetchOptions()
                    .setMethod(HttpMethod.GET)
                    .setMode(CorsMode.NO_CORS)
                )
                .onFailure(Console::log)
                .onSuccess(text -> urlPattern = text)
                .onComplete(ar -> urlPatternPromise.complete());
        });
    }

    @Override
    public String urlPattern() {
        return urlPattern;
    }

    @Override
    public Future<Void> readyFuture() {
        return urlPatternFuture;
    }

    // Helper method to ensure that we return the future only when urlPattern is loaded (because the client may need to
    // call CloudImageService.url() method - which requires urlPattern to be loaded - after returning the future).
    private <T> Future<T> whenUrlPatternLoaded(Future<T> future) {
        return urlPatternFuture.compose(v -> future);
    }

    public Future<Boolean> exists(String id) {
        return whenUrlPatternLoaded(Fetch.fetch(existsUrl, new FetchOptions()
            .setMethod(HttpMethod.POST)
            .setMode(CorsMode.NO_CORS)
            .setBody(new FormData().append("id", id))
        ).compose(response -> {
            if (response.ok())
                return Future.succeededFuture(response.status() == HttpResponseStatus.OK_200); // OK_200 = exists, NO_CONTENT_204 = doesn't exist
            return Future.failedFuture("Failed to call " + existsUrl + ", status = " + response.statusText());
        }));
    }

    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        return whenUrlPatternLoaded(Fetch.fetch(uploadUrl, new FetchOptions()
            .setMethod(HttpMethod.POST)
            .setMode(CorsMode.NO_CORS)
            .setBody(new FormData()
                .append("id", id)
                .append("overwrite", overwrite)
                .append("file", blob, id)
            )
        ).mapEmpty());
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return whenUrlPatternLoaded(Fetch.fetch(deleteUrl, new FetchOptions()
            .setMethod(HttpMethod.POST)
            .setMode(CorsMode.NO_CORS)
            .setBody(new FormData()
                .append("id", id)
                .append("invalidate", invalidate)
            )
        ).mapEmpty());
    }

}
