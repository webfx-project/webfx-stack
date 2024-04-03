package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.cloud.cloudinary.Search;
import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;
import dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop.JsSearch;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clSearch implements Search {

    private final JsSearch jsSearch;

    public GwtJ2clSearch(JsSearch jsSearch) {
        this.jsSearch = jsSearch;
    }

    @Override
    public Search expression(String value) {
        jsSearch.expression(value);
        return this;
    }

    @Override
    public Future<ApiResponse> execute() {
         Promise<ApiResponse> promise = Promise.promise();
         jsSearch.execute()
                 .onFailure(e -> promise.fail(e))
                 .onSuccess(jsApiResponse -> promise.complete(new GwtJ2clApiResponse(jsApiResponse)));
         return promise.future();
    }
}
