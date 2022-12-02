package dev.webfx.stack.conf;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.conf.spi.*;
import dev.webfx.stack.conf.spi.impl.ConfigurationServiceProviderImpl;

import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

/**
 * @author Bruno Salmon
 */
public final class ConfigurationService {

    public static ConfigurationServiceProvider getProvider() {
        ConfigurationServiceProvider provider = SingleServiceProvider.getProvider(dev.webfx.stack.conf.spi.ConfigurationServiceProvider.class, () -> ServiceLoader.load(ConfigurationServiceProvider.class), SingleServiceProvider.NotFoundPolicy.RETURN_NULL);
        if (provider == null)
            SingleServiceProvider.registerServiceProvider(ConfigurationServiceProvider.class, provider = new ConfigurationServiceProviderImpl());
        return provider;
    }

    public static void registerConfigurationFormat(ConfigurationFormat format) {
        getProvider().registerConfigurationFormat(format);
    }

    public static void registerConfigurationSupplier(ConfigurationSupplier supplier) {
        getProvider().registerConfigurationSupplier(supplier);
    }

    public static void registerConfigurationConsumer(ConfigurationConsumer consumer) {
        getProvider().registerConfigurationConsumer(consumer);
    }

    public static List<String> getRegisteredFormatExtensions() {
        return getProvider().getRegisteredFormatExtensions();
    }

    public static ReadOnlyKeyObject readConfiguration(String configName) {
        return readConfiguration(configName, true);
    }

    public static ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        return getProvider().readConfiguration(configName, resolveVariables);
    }

    public static ReadOnlyKeyObject readConfigurationText(String configText, String formatExtension) {
        return readConfigurationText(configText, formatExtension, true);
    }

    public static ReadOnlyKeyObject readConfigurationText(String configText, String formatExtension, boolean resolveVariables) {
        return getProvider().readConfigurationText(configText, formatExtension, resolveVariables);
    }

    public static Future<Void> writeConfiguration(String configName, ReadOnlyJsonObject config) {
        return getProvider().writeConfiguration(configName, config);
    }

    public static String writeConfigurationText(ReadOnlyJsonObject config, String formatExtension) {
        return getProvider().writeConfigurationText(config, formatExtension);
    }

    // Utility methods
    public static String getExtension(String fileName) {
        return fileName == null ? null : fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean areValuesNullOrResolved(String... values) {
        for (String value : values)
            if (value != null && VARIABLE_PATTERN.matcher(value).find())
                return false;
        return true;
    }

    public static boolean areValuesNonNullAndResolved(String... values) {
        for (String value : values)
            if (value == null || VARIABLE_PATTERN.matcher(value).find())
                return false;
        return true;
    }

    public static Pattern VARIABLE_PATTERN = Pattern.compile("\\$?\\{\\{(.+)}}");

}
