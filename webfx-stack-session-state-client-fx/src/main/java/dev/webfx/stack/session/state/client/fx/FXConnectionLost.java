package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXConnectionLost {

    private final static BooleanProperty connectionLostProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXConnectionLost = " + get());
        }
    };

    public static ReadOnlyBooleanProperty connectionLostProperty() {
        return connectionLostProperty;
    }

    public static boolean isConnectionLost() {
        return connectionLostProperty.get();
    }

    // refreshValue() is called by FXConnected & FXConnectionSequence when their value changes
    static void refreshValue() {
        // Connection is lost when we are not connected anymore after the initial connection was established (sequence 1)
        // (sequence 0 refers to the initial non-connected state before initial connection, so it's not a connection loss)
        setConnectionLost(!FXConnected.isConnected() && FXConnectionSequence.getConnectionSequence() >= 1);
    }

    private static void setConnectionLost(boolean connectionLost) {
        connectionLostProperty.set(connectionLost);
    }

}
