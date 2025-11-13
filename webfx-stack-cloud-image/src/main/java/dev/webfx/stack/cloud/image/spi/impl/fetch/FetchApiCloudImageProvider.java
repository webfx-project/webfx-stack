package dev.webfx.stack.cloud.image.spi.impl.fetch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.CloudImageProviderBase;

import static dev.webfx.platform.util.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author Bruno Salmon
 */
public abstract class FetchApiCloudImageProvider extends CloudImageProviderBase {

    public FetchApiCloudImageProvider(String configPath) {
        super(configPath);
    }

    // Public API (authentication not required)

    public Future<Boolean> exists(String id) {
        return Fetch.fetch(url(id, -1, -1), new FetchOptions().setMethod(HttpMethod.HEAD))
            .map(Response::ok);
    }

    // For private API (authentication may be required)

    protected Future<Response> fetchApi(String url, String method) {
        return fetchApi(url, method, null);
    }

    protected Future<Response> fetchApi(String url, String method, Object body) {
        return fetchApi(url, method, body, null);
    }

    protected Future<Response> fetchApi(String url, String method, Object body, String contentType) {
        return whenUrlPatternLoaded(Fetch.fetch(url, createApiFetchOptions(method, body, contentType)));
    }

    protected FetchOptions createApiFetchOptions(String method, Object body, String contentType) {
        FetchOptions fetchOptions = createFetchOptions(method)
            .setHeaders(createApiHeaders(contentType));
        if (body instanceof FormData formData)
            fetchOptions.setBody(formData);
        else if (body instanceof Blob blob)
            fetchOptions.setBody(blob);
        return fetchOptions;
    }

    protected FetchOptions createFetchOptions(String method) {
        return new FetchOptions()
            .setMode(CorsMode.NO_CORS)
            .setMethod(method);
    }

    // Override this if authentication is required
    protected Headers createApiHeaders(String contentType) {
        Headers headers = Fetch.createHeaders();
        if (contentType != null)
            headers.set(CONTENT_TYPE, contentType);
        return headers;
    }

}
