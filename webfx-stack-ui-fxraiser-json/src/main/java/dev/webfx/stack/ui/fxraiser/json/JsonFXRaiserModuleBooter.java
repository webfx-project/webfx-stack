package dev.webfx.stack.ui.fxraiser.json;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonArray;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import dev.webfx.stack.ui.fxraiser.impl.ValueConverterRegistry;
import javafx.scene.shape.SVGPath;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class JsonFXRaiserModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-ui-fxraiser-json";
    }

    @Override
    public int getBootLevel() {
        return APPLICATION_LAUNCH_LEVEL;
    }

    @Override
    public void bootModule() {
        // Adding String to Json possible conversion in FXRaiser
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                if (value instanceof String) {
                    String s = (String) value;
                    if (s.startsWith("{") && s.endsWith("}") && isAssignableFrom(raisedClass, WritableJsonObject.class))
                        return (T) Json.parseObjectSilently(s);
                    if (s.startsWith("[") && s.endsWith("]") && isAssignableFrom(raisedClass, WritableJsonArray.class))
                        return (T) Json.parseArraySilently(s);
                }
                return null;
            }
        });
        // Adding Json to SVGPath possible conversion in FXRaiser
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                if (value instanceof JsonObject && isAssignableFrom(raisedClass, SVGPath.class))
                    return (T) JsonSVGPath.createSVGPath((JsonObject) value);
                return null;
            }
        });
    }
}
