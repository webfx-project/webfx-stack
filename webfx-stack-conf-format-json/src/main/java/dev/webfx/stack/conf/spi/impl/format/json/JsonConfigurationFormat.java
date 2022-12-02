package dev.webfx.stack.conf.spi.impl.format.json;

import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.spi.ConfigurationFormat;

/**
 * @author Bruno Salmon
 */
public final class JsonConfigurationFormat implements ConfigurationFormat {

    @Override
    public String formatExtension() {
        return "json";
    }

    @Override
    public ReadOnlyKeyObject readConfigurationText(String configText) {
        return Json.parseObject(configText);
    }

    @Override
    public String writeConfigurationText(ReadOnlyKeyObject config) {
        return ((ReadOnlyJsonObject) config).toJsonString();
    }

}
