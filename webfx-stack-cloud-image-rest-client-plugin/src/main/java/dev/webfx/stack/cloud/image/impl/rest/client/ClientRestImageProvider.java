package dev.webfx.stack.cloud.image.impl.rest.client;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.platform.util.http.HttpResponseStatus;
import dev.webfx.stack.cloud.image.spi.impl.fetch.FetchApiCloudImageProvider;

/**
 * @author Bruno Salmon
 */
public final class ClientRestImageProvider extends FetchApiCloudImageProvider {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.rest";

    private String existsUrl;
    private String uploadUrl;
    private String deleteUrl;

    public ClientRestImageProvider() {
        super(CONFIG_PATH);
    }

    protected void onConfigLoaded(Config config) {
        existsUrl = config.getString("existsUrl");
        uploadUrl = config.getString("uploadUrl");
        deleteUrl = config.getString("deleteUrl");
        String urlPatternUrl = config.getString("urlPatternUrl");
        Fetch.fetchText(urlPatternUrl, createFetchOptions(HttpMethod.GET))
            .onFailure(Console::error)
            .onSuccess(this::setUrlPattern);
    }

    public Future<Boolean> exists(String id) {
        return fetchApi(existsUrl, HttpMethod.POST, new FormData()
            .append("id", id)
        ).compose(response -> {
            if (response.ok())
                return Future.succeededFuture(response.status() == HttpResponseStatus.OK_200); // OK_200 = exists, NO_CONTENT_204 = doesn't exist
            return Future.failedFuture("Failed to call " + existsUrl + ", status = " + response.statusText());
        });
    }

    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        return fetchApi(uploadUrl, HttpMethod.POST, new FormData()
            .append("id", id)
            .append("overwrite", overwrite)
            .append("file", blob, id)
        ).mapEmpty();
    }

    public Future<Void> delete(String id, boolean invalidate) {
        return fetchApi(deleteUrl, HttpMethod.POST, new FormData()
            .append("id", id)
            .append("invalidate", invalidate)
        ).mapEmpty();
    }

}
