package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXLoggedIn {

    private final static BooleanProperty loggedInProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXLoggedIn = " + get());
        }
    };

    public static ReadOnlyBooleanProperty loggedInProperty() {
        return loggedInProperty;
    }

    public static boolean isLoggedIn() {
        return loggedInProperty.get();
    }

    static void setLoggedIn(boolean loggedIn) {
        loggedInProperty.set(loggedIn);
    }

}
