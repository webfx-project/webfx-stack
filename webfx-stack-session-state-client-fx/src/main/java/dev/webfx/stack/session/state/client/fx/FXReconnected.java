package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXReconnected {

    private final static BooleanProperty reconnectedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXReconnected = " + get());
        }
    };

    public static ReadOnlyBooleanProperty reconnectedProperty() {
        return reconnectedProperty;
    }

    public static boolean isReconnected() {
        return reconnectedProperty.get();
    }

    // refreshValue() is called by FXConnected & FXConnectionSequence when their value changes
    static void refreshValue() {
        // A reconnection is when we are connected again after the initial connection was established (sequence 1)
        setReconnected(FXConnected.isConnected() && FXConnectionSequence.getConnectionSequence() >= 2);
    }

    private static void setReconnected(boolean reconnected) {
        reconnectedProperty.set(reconnected);
    }

}
