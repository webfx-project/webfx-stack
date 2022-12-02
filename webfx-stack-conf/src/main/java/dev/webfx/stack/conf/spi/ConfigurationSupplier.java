package dev.webfx.stack.conf.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;

import java.util.Optional;

public interface ConfigurationSupplier {

    default Future<Void> boot() { return Future.succeededFuture(); }

    Optional<String> resolveVariable(String variableName);

    boolean canReadConfiguration(String configName);

    ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables);

    boolean canWriteConfiguration(String configName);

    Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config);

}
