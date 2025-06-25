package dev.webfx.stack.ui.fxraiser.json;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.ui.fxraiser.FXRaiser;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import dev.webfx.stack.ui.fxraiser.impl.ValueConverterRegistry;
import dev.webfx.stack.ui.json.JsonSVGPath;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class JsonFXRaiserModuleBooter implements ApplicationModuleBooter {

    private final static Class<?> jsonObjectClass = AST.createObject().getClass();
    private final static Class<?> jsonArrayClass = AST.createArray().getClass();

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
        // Adding String to JSON possible conversion in FXRaiser
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                if (value instanceof String) {
                    String s = ((String) value).trim();
                    if (s.startsWith("{") && s.endsWith("}") && isAssignableFrom(raisedClass, jsonObjectClass))
                        return (T) Json.parseObjectSilently(s);
                    if (s.startsWith("[") && s.endsWith("]") && isAssignableFrom(raisedClass, jsonArrayClass))
                        return (T) Json.parseArraySilently(s);
                }
                return null;
            }
        });
        // Adding JSON to SVGPath possible conversion in FXRaiser
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                // Converting JSON object graphic to SVGPath
                if (AST.isObject(value) && isAssignableFrom(raisedClass, SVGPath.class))
                    return (T) JsonSVGPath.createSVGPath((ReadOnlyAstObject) value);
                // Converting JSON array graphic to a Group with all nodes inside
                if (AST.isArray(value) && isAssignableFrom(raisedClass, Group.class)) {
                    ReadOnlyAstArray array = (ReadOnlyAstArray) value;
                    int n = array.size();
                    Node[] graphics = new Node[n];
                    for (int i = 0; i < n; i++)
                        graphics[i] = FXRaiser.raiseToNode(array.getElement(i), args);
                    return (T) new Group(graphics);
                }
                return null;
            }
        });
    }
}
