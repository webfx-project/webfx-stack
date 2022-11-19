package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXConnected {

    private final static BooleanProperty connectedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FxConnected = " + get());
        }
    };

    public static ReadOnlyBooleanProperty connectedProperty() {
        return connectedProperty;
    }

    public static boolean isConnected() {
        return connectedProperty.get();
    }

    static void setConnected(boolean connected) {
       connectedProperty.set(connected);
    }

    static {
        FXInit.init();
    }

}
