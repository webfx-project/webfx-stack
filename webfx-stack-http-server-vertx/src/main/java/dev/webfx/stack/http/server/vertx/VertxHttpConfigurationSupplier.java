package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;

import java.util.Optional;

import static dev.webfx.stack.http.server.vertx.VertxHttpConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class VertxHttpConfigurationSupplier implements ConfigurationSupplier {

    @Override
    public Optional<String> resolveVariable(String variableName) {
        switch (variableName) {
            case "HTTP_SERVER_PORT": return Optional.of(HTTP_SERVER_PORT);
            case "HTTP_SERVER_PROTOCOL": return Optional.of(HTTP_SERVER_PROTOCOL);
            case "HTTP_SERVER_ORIGIN": return Optional.of(HTTP_SERVER_ORIGIN);
            default: return Optional.empty();
        }
    }

    @Override
    public boolean canReadConfiguration(String configName) {
        return false;
    }

    @Override
    public ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        return null;
    }

    @Override
    public boolean canWriteConfiguration(String configName) {
        return false;
    }

    @Override
    public Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config) {
        return Future.failedFuture("Can't write configuration");
    }
}
