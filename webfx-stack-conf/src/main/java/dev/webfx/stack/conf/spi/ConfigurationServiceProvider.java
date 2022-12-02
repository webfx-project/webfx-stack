package dev.webfx.stack.conf.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;

import java.util.List;

public interface ConfigurationServiceProvider {

    void boot();

    void registerConfigurationFormat(ConfigurationFormat format);

    void registerConfigurationSupplier(ConfigurationSupplier supplier);

    void registerConfigurationConsumer(ConfigurationConsumer consumer);

    List<String> getRegisteredFormatExtensions();

    ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables);

    ReadOnlyKeyObject readConfigurationText(String configText, String formatExtension, boolean resolveVariables);

    Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config);

    String writeConfigurationText(ReadOnlyJsonObject config, String formatExtension);

}
