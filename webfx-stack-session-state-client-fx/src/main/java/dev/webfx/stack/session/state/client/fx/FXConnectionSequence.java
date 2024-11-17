package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;

/**
 * @author Bruno Salmon
 */
public final class FXConnectionSequence {

    private final static IntegerProperty connectionSequence = FXProperties.newIntegerProperty(FXConnected.isConnected() ? 1 : 0, seq -> {
        Console.log("FxConnectionSequence = " + seq);
        FXConnectionLost.refreshValue();
        FXReconnected.refreshValue();
    });

    public static int getConnectionSequence() {
        return connectionSequence.get();
    }

    public static ReadOnlyIntegerProperty connectionSequenceProperty() {
        return connectionSequence;
    }

    private static void setConnectionSequence(int connectionSequence) {
        FXConnectionSequence.connectionSequence.set(connectionSequence);
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

    static void init() { // Called back (only once) by FXInit in a controlled overall sequence
        FXProperties.runOnPropertyChange(connected -> {
            if (connected)
                setConnectionSequence(getConnectionSequence() + 1);
        }, FXConnected.connectedProperty());
    }
}
