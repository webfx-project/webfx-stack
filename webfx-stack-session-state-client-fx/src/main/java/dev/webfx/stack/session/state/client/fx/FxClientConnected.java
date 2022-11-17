package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FxClientConnected {

    private final static BooleanProperty clientConnectedProperty = new SimpleBooleanProperty() {
        @Override
        protected void invalidated() {
            Console.log("FxClientConnected = " + get());
        }
    };

    public static BooleanProperty clientConnectedProperty() {
        return clientConnectedProperty;
    }

    public static boolean isClientConnected() {
        return clientConnectedProperty.get();
    }

    public static void setClientConnected(boolean connected) {
       clientConnectedProperty.set(connected);
    }

    static {
        FxInit.init();
    }

}
