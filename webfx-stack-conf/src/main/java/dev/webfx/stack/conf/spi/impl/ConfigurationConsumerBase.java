package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.ast.ReadOnlyAstObject;
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

    protected ReadOnlyAstObject readConfiguration() {
        return ConfigurationService.readConfiguration(getConfigurationName(), true);
    }
}
