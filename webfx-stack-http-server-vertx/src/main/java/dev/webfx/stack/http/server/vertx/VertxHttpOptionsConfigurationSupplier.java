package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.util.serviceloader.MultipleServiceProviders;
import dev.webfx.stack.conf.spi.ConfigurationConsumer;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;

import java.util.Optional;
import java.util.ServiceLoader;

import static dev.webfx.stack.http.server.vertx.VertxHttpOptionsConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class VertxHttpOptionsConfigurationSupplier implements ConfigurationSupplier {

    public VertxHttpOptionsConfigurationSupplier() {
        // Hack to ensure that VertxHttpConfigurationConsumer will be called first when registering the configuration
        // consumers (in ConfigurationServiceProviderImpl.boot() method). In this way, the variables HTTP_SERVER_PORT,
        // HTTP_SERVER_PROTOCOL, and HTTP_SERVER_ORIGIN will be resolved first by VertxHttpConfigurationConsumer, and
        // subsequent configuration consumers can then resolve these variables through this configuration supplier.
        // 1) Getting the list of all configuration consumers, (ConfigurationServiceProviderImpl will use that same
        // list instance because MultipleServiceProviders caches it)
        MultipleServiceProviders.getProviders(ConfigurationConsumer.class, () -> ServiceLoader.load(ConfigurationConsumer.class))
                // 2) sorting that list, so that VertxHttpConfigurationConsumer comes in first position
                .sort((o1, o2) -> o1 instanceof VertxHttpOptionsConfigurationConsumer ? -1 : o2 instanceof VertxHttpOptionsConfigurationConsumer ? 1 : 0);
    }

    @Override
    public Optional<String> resolveVariable(String variableName) {
        // This method can be called when configuration consumers try to resolve variables, and at this point, the
        // variables HTTP_SERVER_PORT, HTTP_SERVER_PROTOCOL, and HTTP_SERVER_ORIGIN should have already been set thanks
        // to the hack explained in the constructor.
        switch (variableName) {
            case "HTTP_SERVER_PORT": return Optional.ofNullable(HTTP_SERVER_PORT);
            case "HTTP_SERVER_PROTOCOL": return Optional.ofNullable(HTTP_SERVER_PROTOCOL);
            case "HTTP_SERVER_ORIGIN": return Optional.ofNullable(HTTP_SERVER_ORIGIN);
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
