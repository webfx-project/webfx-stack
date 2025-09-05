package dev.webfx.stack.shareddata.cache.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.shareddata.AsyncMap;

/**
 * @author Bruno Salmon
 */
public interface CachesProvider {

    Future<AsyncMap<String, Object>> getCache();

}
