package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXReconnected {

    private final static BooleanProperty reconnectedProperty = FXProperties.newBooleanProperty(reconnected ->
        Console.log("FXReconnected = " + reconnected)
    );

    public static ReadOnlyBooleanProperty reconnectedProperty() {
        return reconnectedProperty;
    }

    public static boolean isReconnected() {
        return reconnectedProperty.get();
    }

    private static void setReconnected(boolean reconnected) {
        reconnectedProperty.set(reconnected);
    }

    // refreshValue() is called by FXConnected & FXConnectionSequence when their value changes
    static void refreshValue() {
        // A reconnection is when we are connected again after the initial connection was established (sequence 1)
        setReconnected(FXConnected.isConnected() && FXConnectionSequence.getConnectionSequence() >= 2);
    }

}
