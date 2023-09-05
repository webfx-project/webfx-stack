package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public abstract class NoDefaultConfigurationConsumerBase extends ConfigurationConsumerBase {

    public NoDefaultConfigurationConsumerBase(String configurationName) {
        super(configurationName);
    }

    @Override
    public ReadOnlyAstObject getDefaultConfiguration() {
        return null;
    }
}
