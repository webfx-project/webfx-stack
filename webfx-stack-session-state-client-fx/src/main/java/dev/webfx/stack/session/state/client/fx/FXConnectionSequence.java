package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * @author Bruno Salmon
 */
public final class FXConnectionSequence {

    private final static IntegerProperty connectionSequence = new SimpleIntegerProperty(FXConnected.isConnected() ? 1 : 0) {
        @Override
        protected void invalidated() {
            Console.log("FxConnectionSequence = " + get());
            FXConnectionLost.refreshValue();
            FXReconnected.refreshValue();
        }
    };

    public static int getConnectionSequence() {
        return connectionSequence.get();
    }

    public static ReadOnlyIntegerProperty connectionSequenceProperty() {
        return connectionSequence;
    }

    private static void setConnectionSequence(int connectionSequence) {
        FXConnectionSequence.connectionSequence.set(connectionSequence);
    }

    static void init() {
        FXConnected.connectedProperty().addListener((observable, oldValue, connected) -> {
            if (connected)
                setConnectionSequence(getConnectionSequence() + 1);
        });
    }
}
