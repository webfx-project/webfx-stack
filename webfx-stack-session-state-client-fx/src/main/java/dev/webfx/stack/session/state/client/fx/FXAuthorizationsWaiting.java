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
        FXProperties.runOnPropertyChange(loggedOut -> {
            if (isAuthorizationsWaiting() || loggedOut)
                runnable.run();
        }, FXLoggedOut.loggedOutProperty());
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

    static void init() { // Called back (only once) by FXInit in a controlled overall sequence
        FXProperties.runNowAndOnPropertyChange(FXAuthorizationsWaiting::updateAuthorizationsWaiting, FXLoggedOut.loggedOutProperty());
        FXAuthorizationsChanged.runOnAuthorizationsChanged(FXAuthorizationsWaiting::updateAuthorizationsWaiting);
    }

}
