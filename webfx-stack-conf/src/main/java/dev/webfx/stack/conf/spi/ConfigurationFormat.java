package dev.webfx.stack.conf.spi;

import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;

public interface ConfigurationFormat {

    String formatExtension();

    ReadOnlyKeyObject readConfigurationText(String configText);

    String writeConfigurationText(ReadOnlyKeyObject config);

}
