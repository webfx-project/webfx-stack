package dev.webfx.stack.cloud.image.impl.fetchbased;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.fetch.CorsMode;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.stack.cloud.image.CloudImageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public abstract class FetchBasedCloudImageService implements CloudImageService {

    protected Headers createHeaders() {
        return Fetch.createHeaders()
                .set("Content-Type", "application/json")
                .set("charset", "UTF-8")
                ;
    }

    protected <T> Future<T> fetchAndConvertJsonObject(String url, String method, FetchOptions options, Function<ReadOnlyAstObject, T> converter) {
        Promise<T> promise = Promise.promise();
        Headers headers = createHeaders();
        options.setHeaders(headers)
               .setMethod(method)
               .setMode(CorsMode.CORS);
        JsonFetch.fetchJsonObject(url, options)
                .onFailure(promise::fail)
                .onSuccess(o -> {
                    String error = getJsonErrorMessage(o);
                    if (error != null) {
                        promise.fail(error);
                        return;
                    }
                    T result = converter.apply(o);
                    promise.complete(result);
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

    protected static Map astObjectToMap(ReadOnlyAstObject o) {
        Map map = new HashMap();
        for (Object key : o.keys()) {
            Object value = o.get((String) key);
            map.put(key, astValueToMapList(value));
        }
        return map;
    }

    protected static Object astValueToMapList(Object value) {
        if (AST.isObject(value))
            return astObjectToMap((ReadOnlyAstObject) value);
        if (AST.isArray(value))
            return astArrayToList((ReadOnlyAstArray) value);
        return value;
    }

    protected static List astArrayToList(ReadOnlyAstArray a) {
        List list = new ArrayList();
        for (Object value : a) {
            list.add(astValueToMapList(value));
        }
        return list;
    }

}
