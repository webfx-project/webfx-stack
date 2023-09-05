package dev.webfx.stack.conf.spi.impl.format.json;

import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
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
    public ReadOnlyAstObject readConfigurationText(String configText) {
        return Json.parseObject(configText);
    }

    @Override
    public String writeConfigurationText(ReadOnlyAstObject config) {
        return ((ReadOnlyJsonObject) config).toJsonString();
    }

}
