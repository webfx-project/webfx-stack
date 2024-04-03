package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;
import dev.webfx.stack.cloud.cloudinary.Search;

/**
 * @author Bruno Salmon
 */
public class JavaSearch implements Search {

    private final com.cloudinary.Search jSearch;

    public JavaSearch(com.cloudinary.Search jSearch) {
        this.jSearch = jSearch;
    }

    @Override
    public Search expression(String value) {
        jSearch.expression(value);
        return this;
    }

    @Override
    public Future<ApiResponse> execute() {
        Promise<ApiResponse> promise = Promise.promise();
        Scheduler.runInBackground(() -> {
            try {
                com.cloudinary.api.ApiResponse result = jSearch.execute();
                promise.complete(new JavaApiResponse(result));
            } catch (Exception e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }
}
