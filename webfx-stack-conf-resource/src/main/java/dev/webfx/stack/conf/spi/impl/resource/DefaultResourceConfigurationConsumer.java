package dev.webfx.stack.conf.spi.impl.resource;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.ConfigurationConsumerBase;

/**
 * @author Bruno Salmon
 */
public abstract class DefaultResourceConfigurationConsumer extends ConfigurationConsumerBase {

    private final String defaultResourceFileName;
    private ReadOnlyKeyObject defaultConfiguration;

    public DefaultResourceConfigurationConsumer(String defaultResourceFileName) {
        this(null, defaultResourceFileName);
    }

    public DefaultResourceConfigurationConsumer(String configurationName, String defaultResourceFileName) {
        super(configurationName);
        this.defaultResourceFileName = defaultResourceFileName;
    }

    @Override
    public Future<Void> boot() {
        String resourcePath = Resource.toUrl(defaultResourceFileName, getClass());
        Promise<Void> promise = Promise.promise();
        Resource.loadText(resourcePath,
                /* On success: */ configText -> {
                    defaultConfiguration = ConfigurationService.readConfigurationText(configText, defaultResourceFileName);
                    promise.handle(boot(readConfiguration()));
                }, /* On error: */ promise::fail);
        return promise.future();
    }

    protected abstract Future<Void> boot(ReadOnlyKeyObject config);

    @Override
    public ReadOnlyKeyObject getDefaultConfiguration() {
        return defaultConfiguration;
    }

}
