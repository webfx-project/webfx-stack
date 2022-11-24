package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXAuthorizationsChanged {

    private final static BooleanProperty authorizationsChangedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXAuthorizationsChanged = " + get());
        }
    };
    private static boolean fireScheduled;

    public static ReadOnlyBooleanProperty authorizationsChangedProperty() {
        return authorizationsChangedProperty;
    }

    public static boolean hasAuthorizationsChanged() {
        return authorizationsChangedProperty.get();
    }

    /**
     * "If we haven't already scheduled a call to fire the authorizationsChangedProperty, then schedule a call to fire the
     * authorizationsChangedProperty."
     *
     * The authorizationsChangedProperty is a JavaFX property that is used to notify the UI that the authorizations have
     * changed
     */
    public static void fireAuthorizationsChanged() {
        if (!fireScheduled) {
            fireScheduled = true;
            UiScheduler.runInUiThread(() -> {
                authorizationsChangedProperty.set(true);
                authorizationsChangedProperty.set(false);
                fireScheduled = false;
            });
        }
    }

    public static void runOnAuthorizationsChanged(Runnable runnable) {
        FXProperties.runOnPropertiesChange(() -> {
            if (hasAuthorizationsChanged()) // Only on false -> true transition
                runnable.run();
        }, authorizationsChangedProperty());
    }

}
