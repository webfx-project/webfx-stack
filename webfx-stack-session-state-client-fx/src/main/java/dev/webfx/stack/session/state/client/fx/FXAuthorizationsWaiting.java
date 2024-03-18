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
public final class FXAuthorizationsWaiting {

    private final static BooleanProperty authorizationsWaitingProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            Console.log("FXAuthorizationsWaiting = " + get());
        }
    };

    public static ReadOnlyBooleanProperty authorizationsWaitingProperty() {
        return authorizationsWaitingProperty;
    }

    public static boolean isAuthorizationsWaiting() {
        return authorizationsWaitingProperty.get();
    }

    private static void setAuthorizationsWaiting(boolean authorizationsWaiting) {
        authorizationsWaitingProperty.set(authorizationsWaiting);
    }

    private static void updateAuthorizationsWaiting() {
        setAuthorizationsWaiting(!FXLoggedOut.isLoggedOut() && !Objects.equals(FXUserId.getUserId(), FXAuthorizationsChanged.authorizationsUserId));
    }

    public static void runOnAuthorizationsChangedOrWaiting(Runnable runnable) {
        FXAuthorizationsChanged.runOnAuthorizationsChanged(runnable);
        FXProperties.runOnPropertiesChange(() -> {
            if (isAuthorizationsWaiting())
                runnable.run();
        }, FXLoggedOut.loggedOutProperty());
    }

    static {
        FXProperties.runNowAndOnPropertiesChange(FXAuthorizationsWaiting::updateAuthorizationsWaiting, FXLoggedOut.loggedOutProperty());
        FXAuthorizationsChanged.runOnAuthorizationsChanged(FXAuthorizationsWaiting::updateAuthorizationsWaiting);
    }

}
