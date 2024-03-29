package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXAuthorizationsReceived {

    private final static BooleanProperty authorizationsReceivedProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXAuthorizationsReceived = " + get());
        }
    };

    public static ReadOnlyBooleanProperty authorizationsReceivedProperty() {
        return authorizationsReceivedProperty;
    }

    public static boolean isAuthorizationsReceived() {
        return authorizationsReceivedProperty.get();
    }

    private static void setAuthorizationsReceived(boolean authorizationsReceived) {
        authorizationsReceivedProperty.set(authorizationsReceived);
    }

    private static void updateAuthorizationsReceived() {
        if (FXLoggedOut.isLoggedOut() || !Objects.equals(FXUserId.getUserId(), FXAuthorizationsChanged.authorizationsUserId))
            setAuthorizationsReceived(false);
        else if (FXAuthorizationsChanged.hasAuthorizationsChanged())
            setAuthorizationsReceived(true);
    }

    static {
        FXProperties.runNowAndOnPropertiesChange(FXAuthorizationsReceived::updateAuthorizationsReceived, FXLoggedOut.loggedOutProperty());
        FXAuthorizationsChanged.runOnAuthorizationsChanged(FXAuthorizationsReceived::updateAuthorizationsReceived);
    }

}
