package dev.webfx.stack.cloud.image.spi.impl.jsonfetchapi;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.fetch.CorsMode;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.stack.cloud.image.spi.CloudImageProvider;

import static dev.webfx.platform.util.http.HttpHeaders.*;

/**
 * @author Bruno Salmon
 */
public abstract class JsonFetchApiCloudImageProvider implements CloudImageProvider {

    protected Headers createJsonApiHeaders() {
        return Fetch.createHeaders()
                .set(CONTENT_TYPE, APPLICATION_JSON_UTF8)
                ;
    }

    protected Future<ReadOnlyAstObject> fetchJsonApiObject(String url, String method, FetchOptions options) {
        Promise<ReadOnlyAstObject> promise = Promise.promise();
        options.setHeaders(createJsonApiHeaders())
               .setMethod(method)
               .setMode(CorsMode.NO_CORS);
        JsonFetch.fetchJsonObject(url, options)
                .onFailure(promise::fail)
                .onSuccess(json -> {
                    String error = getJsonErrorMessage(json);
                    if (error != null) {
                        promise.fail(error);
                        return;
                    }
                    promise.complete(json);
                });
        return promise.future();
    }

    protected String getJsonErrorMessage(ReadOnlyAstObject jsonObject) {
        ReadOnlyAstObject error = jsonObject.getObject("error");
        if (error != null) {
            return error.getString("message");
        }
        return null;
    }

}
