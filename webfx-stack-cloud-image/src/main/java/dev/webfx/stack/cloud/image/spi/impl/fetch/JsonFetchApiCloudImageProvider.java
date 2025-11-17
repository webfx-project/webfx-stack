package dev.webfx.stack.cloud.image.spi.impl.fetch;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.platform.util.http.HttpHeaders;

/**
 * @author Bruno Salmon
 */
public abstract class JsonFetchApiCloudImageProvider extends FetchApiCloudImageProvider {

    public JsonFetchApiCloudImageProvider(String configPath) {
        super(configPath);
    }

    protected Future<ReadOnlyAstObject> fetchJsonApiObject(String url, Object body, String method) {
        return whenUrlPatternLoaded(JsonFetch.fetchJsonObject(url, createApiFetchOptions(method, body, HttpHeaders.APPLICATION_JSON_UTF8)));
    }


}
