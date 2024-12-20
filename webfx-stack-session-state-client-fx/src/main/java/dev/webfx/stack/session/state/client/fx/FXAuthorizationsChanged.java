package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXAuthorizationsChanged {

    private final static BooleanProperty authorizationsChangedProperty = FXProperties.newBooleanProperty(changed -> {
        //Console.log("FXAuthorizationsChanged = " + changed);
    });
    private static boolean fireScheduled;

    public static ReadOnlyBooleanProperty authorizationsChangedProperty() {
        return authorizationsChangedProperty;
    }

    public static boolean hasAuthorizationsChanged() {
        return authorizationsChangedProperty.get();
    }

    static Object authorizationsUserId;

    /**
     * "If we haven't already scheduled a call to fire the authorizationsChangedProperty, then schedule a call to fire the
     * authorizationsChangedProperty."
     * <p>
     * The authorizationsChangedProperty is a JavaFX property that is used to notify the UI that the authorizations have
     * changed
     */
    public static void fireAuthorizationsChanged() {
        if (!fireScheduled) {
            fireScheduled = true;
            UiScheduler.runInUiThread(() -> {
                authorizationsUserId = FXUserId.getUserId();
                authorizationsChangedProperty.set(true);
                authorizationsChangedProperty.set(false);
                fireScheduled = false;
            });
        }
    }

    public static void runOnAuthorizationsChanged(Runnable runnable) {
        FXProperties.runOnPropertyChange(changed -> {
            if (changed) // Only on false -> true transition
                runnable.run();
        }, authorizationsChangedProperty());
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

}
