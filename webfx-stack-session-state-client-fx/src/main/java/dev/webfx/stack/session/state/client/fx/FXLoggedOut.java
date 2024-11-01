package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.LogoutUserId;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class FXLoggedOut {

    private final static BooleanProperty loggedOutProperty = FXProperties.newBooleanProperty(loggedOut ->
        Console.log("FXLoggedOut = " + loggedOut)
    );

    public static ReadOnlyBooleanProperty loggedOutProperty() {
        return loggedOutProperty;
    }

    public static boolean isLoggedOut() {
        return loggedOutProperty.get();
    }

    private static void setLoggedOut(boolean loggedOut) {
        loggedOutProperty.set(loggedOut);
    }

    // LoggedOut management: we consider the user is logged out when the userId is null or equals to LOGOUT_USER_ID
    // (which happens when the server pushes a logout). Note that FXLoggedOut is not exactly the negation of FXLoggedIn,
    // because FXLoggedIn doesn't become true just by setting a non-null userId different from LOGOUT_USER_ID. It also
    // requires that the server pushes back the user authorizations. So before this push arrives to the client,
    // FXLoggedIn and FXLoggedOut are both false.

    private static void updateLoggedOut() {
        Object userId = FXUserId.getUserId();
        setLoggedOut(LogoutUserId.isLogoutUserIdOrNull(userId));
    }

    static {
        FXProperties.runNowAndOnPropertiesChange(FXLoggedOut::updateLoggedOut, FXUserId.userIdProperty());
    }

}
