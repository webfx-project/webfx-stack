package dev.webfx.stack.conf.spi.impl;

/**
 * @author Bruno Salmon
 */
public class ConfigurationException extends Exception {

    private final boolean partial;

    public ConfigurationException() {
        this(false);
    }

    public ConfigurationException(boolean partial) {
        this.partial = partial;
    }

    public ConfigurationException(boolean partial, String message) {
        super(message);
        this.partial = partial;
    }

    public boolean isPartial() {
        return partial;
    }
}
