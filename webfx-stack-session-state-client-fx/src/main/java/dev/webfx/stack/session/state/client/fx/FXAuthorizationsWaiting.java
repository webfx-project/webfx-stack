package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXAuthorizationsWaiting {

    private final static BooleanProperty authorizationsWaitingProperty = FXProperties.newBooleanProperty(waiting ->
        Console.log("FXAuthorizationsWaiting = " + waiting)
    );

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
            if (isAuthorizationsWaiting() || FXLoggedOut.isLoggedOut())
                runnable.run();
        }, FXLoggedOut.loggedOutProperty());
    }

    public static void init() {
        // The first call will trigger the static initializer below, and subsequent calls won't do anything
    }

    static {
        FXProperties.runNowAndOnPropertiesChange(FXAuthorizationsWaiting::updateAuthorizationsWaiting, FXLoggedOut.loggedOutProperty());
        FXAuthorizationsChanged.runOnAuthorizationsChanged(FXAuthorizationsWaiting::updateAuthorizationsWaiting);
    }

}
