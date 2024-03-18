package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
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

    private static void setLoggedIn(boolean loggedIn) {
        loggedInProperty.set(loggedIn);
    }

    // LoggedIn management: we consider the user is logged in when 1) the userId is set (but this can come from the
    // client side when restoring the session), and 2) the server consequently pushed the authorizations to the client
    // (which proves by the way that the server approved this userId as authenticated)

    private static void updateLoggedIn() {
        setLoggedIn(FXAuthorizationsReceived.isAuthorizationsReceived());
    }

    static {
        FXProperties.runNowAndOnPropertiesChange(FXLoggedIn::updateLoggedIn, FXAuthorizationsReceived.authorizationsReceivedProperty());
    }

}
