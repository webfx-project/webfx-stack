package dev.webfx.stack.conf.spi.impl.windowlocation;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.windowlocation.WindowLocation;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;
import dev.webfx.stack.conf.spi.HasConfigurationLogInfo;

import java.util.Optional;

/**
 * @author Bruno Salmon
 */
public class WindowLocationConfigurationSupplier implements ConfigurationSupplier, HasConfigurationLogInfo {

    @Override
    public String getLogInfo() {
        return "WINDOW_LOCATION_XXX variables can be used for configuration resolution";
    }

    @Override
    public Optional<String> resolveVariable(String variableName) {
        switch (variableName) {
            case "WINDOW_LOCATION_HOST":
                return Optional.of(WindowLocation.getHost());
            case "WINDOW_LOCATION_HOSTNAME":
                return Optional.of(WindowLocation.getHostname());
            case "WINDOW_LOCATION_PORT":
                return Optional.of(WindowLocation.getPort());
            case "WINDOW_LOCATION_PROTOCOL":
                return Optional.of(WindowLocation.getProtocol());
            case "WINDOW_LOCATION_ORIGIN":
                return Optional.of(WindowLocation.getOrigin());
            case "WINDOW_LOCATION_SSL":
                return Optional.of(String.valueOf(WindowLocation.getProtocol().equalsIgnoreCase("https")));
            default:
                return Optional.empty();
        }
    }

    @Override
    public boolean canReadConfiguration(String configName) {
        return false;
    }

    @Override
    public ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        throw new IllegalArgumentException("No configuration found for " + configName);
    }

    @Override
    public boolean canWriteConfiguration(String configName) {
        return false;
    }

    @Override
    public Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config) {
        throw new IllegalArgumentException("No configuration found for " + configName);
    }
}
