package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.ConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public abstract class ConfigurationConsumerBase implements ConfigurationConsumer {

    private final String configurationName;

    public ConfigurationConsumerBase(String configurationName) {
        this.configurationName = configurationName;
    }

    @Override
    public String getConfigurationName() {
        return configurationName;
    }

    protected ReadOnlyKeyObject readConfiguration() {
        return ConfigurationService.readConfiguration(getConfigurationName(), true);
    }
}
