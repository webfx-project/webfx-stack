package dev.webfx.stack.conf.spi.impl.java.file;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;
import dev.webfx.stack.conf.spi.HasConfigurationLogInfo;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author Bruno Salmon
 */
public class DirectoryConfigurationSupplier implements ConfigurationSupplier, HasConfigurationLogInfo {

    private final File configurationDirectory;

    public DirectoryConfigurationSupplier() {
        this(getDefaultConfigurationDirectory());
    }

    static File getDefaultConfigurationDirectory() {
        return new File(System.getProperty("user.dir") + "/conf");
    }

    public DirectoryConfigurationSupplier(File configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }

    @Override
    public String getLogInfo() {
        String logInfo = "Configuration directory location: " + configurationDirectory.getAbsolutePath();
        if (!configurationDirectory.exists())
            logInfo += "\n⚠️ WARNING: " + configurationDirectory.getAbsolutePath() + " doesn't exist";
        else if (!configurationDirectory.isDirectory())
            logInfo += "\n⚠️ WARNING: " + configurationDirectory.getAbsolutePath() + " is not a directory";
        else try {
                File[] files = configurationDirectory.listFiles();
                if (files == null || files.length == 0)
                    logInfo += "\n⚠️ WARNING: " + configurationDirectory.getAbsolutePath() + " is empty";
            } catch (Exception e) {
                logInfo += "\n⚠️ WARNING: " + configurationDirectory.getAbsolutePath() + " is not accessible";
            }
        return logInfo;
    }

    @Override
    public Optional<String> resolveVariable(String variableName) {
        File[] files = configurationDirectory.listFiles();
        if (files != null) {
            List<String> supportedExtensions = ConfigurationService.getRegisteredFormatExtensions();
            for (File file : files) {
                if (supportedExtensions.contains(ConfigurationService.getExtension(file.getName()))) {
                    ConfigurationFile configurationFile = new ConfigurationFile(file);
                    ReadOnlyKeyObject config = configurationFile.readConfiguration(false);
                    if (config.has(variableName))
                        return Optional.of(config.getString(variableName));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean canReadConfiguration(String configName) {
        return getExistingConfigFile(configName) != null;
    }

    private ConfigurationFile getExistingConfigFile(String configName) {
        return ConfigurationFile.searchExistingConfigurationFileInDirectory(configurationDirectory, configName);
    }

    @Override
    public ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        ConfigurationFile configurationFile = getExistingConfigFile(configName);
        if (configurationFile == null)
            throw new IllegalArgumentException("No configuration file found for " + configName);
        return configurationFile.readConfiguration(resolveVariables);
    }

    @Override
    public boolean canWriteConfiguration(String configName) {
        ConfigurationFile configurationFile = getExistingConfigFile(configName);
        return configurationFile != null && configurationFile.getFile().canWrite();
    }

    @Override
    public Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config) {
        return null;
    }
}
