package dev.webfx.stack.ui.controls;

import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import dev.webfx.kit.util.properties.FXProperties;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class Controls {

    public static void onSkinReady(Control control, Runnable runnable) {
        onSkinReady(control, skin -> runnable.run());
    }

    public static void onSkinReady(Control control, Consumer<Skin<?>> consumer) {
        FXProperties.onPropertySet(control.skinProperty(), consumer);
    }
}
