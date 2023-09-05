package dev.webfx.stack.conf.spi.impl.java.file;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.conf.ConfigurationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Bruno Salmon
 */
public class ConfigurationFile {

    private final File file;

    public ConfigurationFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public ReadOnlyAstObject readConfiguration(boolean resolveVariables) {
        try {
            String configText = Files.readString(file.toPath());
            return ConfigurationService.readConfigurationText(configText, file.getName(), resolveVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConfigurationFile searchExistingConfigurationFileInDirectory(File configurationDirectory, String configName) {
        return ConfigurationService.getRegisteredFormatExtensions().stream()
                .map(extension -> new File(configurationDirectory, configName + "." + extension))
                .filter(File::exists)
                .map(ConfigurationFile::new)
                .findFirst()
                .orElse(null);
    }

}
