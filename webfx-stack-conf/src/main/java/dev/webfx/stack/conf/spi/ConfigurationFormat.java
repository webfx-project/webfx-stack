package dev.webfx.stack.conf.spi;

import dev.webfx.platform.ast.ReadOnlyAstObject;

public interface ConfigurationFormat {

    String formatExtension();

    ReadOnlyAstObject readConfigurationText(String configText);

    String writeConfigurationText(ReadOnlyAstObject config);

}
