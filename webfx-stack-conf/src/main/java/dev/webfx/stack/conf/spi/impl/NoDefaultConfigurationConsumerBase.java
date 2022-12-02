package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;

/**
 * @author Bruno Salmon
 */
public abstract class NoDefaultConfigurationConsumerBase extends ConfigurationConsumerBase {

    public NoDefaultConfigurationConsumerBase(String configurationName) {
        super(configurationName);
    }

    @Override
    public ReadOnlyKeyObject getDefaultConfiguration() {
        return null;
    }
}
