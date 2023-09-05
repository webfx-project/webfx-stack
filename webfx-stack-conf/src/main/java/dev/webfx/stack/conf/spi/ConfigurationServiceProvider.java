package dev.webfx.stack.conf.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;

import java.util.List;

public interface ConfigurationServiceProvider {

    void boot();

    void registerConfigurationFormat(ConfigurationFormat format);

    void registerConfigurationSupplier(ConfigurationSupplier supplier);

    void registerConfigurationConsumer(ConfigurationConsumer consumer);

    List<String> getRegisteredFormatExtensions();

    ReadOnlyAstObject readConfiguration(String configName, boolean resolveVariables);

    ReadOnlyAstObject readConfigurationText(String configText, String formatExtension, boolean resolveVariables);

    Future<Void> writeConfiguration(String configName, ReadOnlyAstObject config);

    String writeConfigurationText(ReadOnlyJsonObject config, String formatExtension);

}
