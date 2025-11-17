package dev.webfx.stack.cloud.image.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.cloud.image.spi.CloudImageProvider;

/**
 * @author Bruno Salmon
 */
public abstract class CloudImageProviderBase extends CloudImageProvider {

    protected final Promise<Void> readyPromise = Promise.promise();
    private String urlPattern;

    public CloudImageProviderBase(String configPath) {
        ConfigLoader.onConfigLoaded(configPath, this::onConfigLoaded);
    }

    protected abstract void onConfigLoaded(Config config);

    @Override
    public Future<Void> readyFuture() {
        return readyPromise.future();
    }

    protected void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
        Console.log("urlPattern set to " + urlPattern);
        readyPromise.complete();
    }

    @Override
    public String urlPattern() {
        return urlPattern;
    }

    // Helper method to ensure that we return the future only when urlPattern is loaded (because the client may need to
    // call CloudImageService.url() method - which requires urlPattern to be loaded - after returning the future).
    protected <T> Future<T> whenUrlPatternLoaded(Future<T> future) {
        return readyFuture().compose(v -> future);
    }

}
