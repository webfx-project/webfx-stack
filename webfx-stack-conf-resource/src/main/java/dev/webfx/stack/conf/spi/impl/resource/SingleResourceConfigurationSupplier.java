package dev.webfx.stack.conf.spi.impl.resource;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;

import java.util.Optional;

/**
 * @author Bruno Salmon
 */
public class SingleResourceConfigurationSupplier implements ConfigurationSupplier {

    private final String configurationName;
    private final String resourceFileName;
    private String configText;
    private ReadOnlyKeyObject unresolvedConfig;
    private ReadOnlyKeyObject resolvedConfig;


    public SingleResourceConfigurationSupplier(String configurationName, String resourceFileName) {
        this.configurationName = configurationName;
        this.resourceFileName = resourceFileName;
    }

    @Override
    public Future<Void> boot() {
        String resourcePath = Resource.toUrl(resourceFileName, getClass());
        Promise<Void> promise = Promise.promise();
        Resource.loadText(resourcePath,
                /* On success: */ configText -> {
                    this.configText = configText;
                    promise.complete();
                }, /* On error: */ promise::fail);
        return promise.future();
    }

    public ReadOnlyKeyObject getUnresolvedConfig() {
        if (unresolvedConfig == null)
            unresolvedConfig = ConfigurationService.readConfigurationText(configText, resourceFileName, false);
        return unresolvedConfig;
    }

    public ReadOnlyKeyObject getResolvedConfig() {
        if (resolvedConfig == null)
            resolvedConfig = ConfigurationService.readConfigurationText(configText, resourceFileName, true);
        return resolvedConfig;
    }

    @Override
    public Optional<String> resolveVariable(String variableName) {
        return getUnresolvedConfig().has(variableName) ? Optional.of(unresolvedConfig.getString(variableName)) : Optional.empty();
    }

    @Override
    public boolean canReadConfiguration(String configName) {
        return configurationName.equals(configName);
    }

    @Override
    public ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        if (!canReadConfiguration(configName))
            return null;
        return resolveVariables ? getResolvedConfig() : getUnresolvedConfig();
    }

    @Override
    public boolean canWriteConfiguration(String configName) {
        return false;
    }

    @Override
    public Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config) {
        return Future.failedFuture("Can't write configuration on resource file");
    }
}
