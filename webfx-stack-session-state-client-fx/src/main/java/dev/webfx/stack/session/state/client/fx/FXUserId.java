package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.*;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXUserId {

    private final static ObjectProperty<Object> userIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Object userId = get();
            Console.log("FxUserId = " + userId);
            ClientSideStateSession.getInstance().changeUserId(userId, false, false);
            updateLoggedIn();
        }
    };

    public static ObjectProperty<Object> userIdProperty() {
        return userIdProperty;
    }

    public static Object getUserId() {
        return userIdProperty.get();
    }

    public static void setUserId(Object userId) {
        if (!Objects.equals(userId, getUserId()))
            userIdProperty.set(userId);
    }

    // LoggedIn management: we consider the user is logged in when 1) the userId is set (but this can come from the
    // client side when restoring the session), and 2) the server consequently pushed the authorizations to the client
    // (which proves by the way that the server approved this userId as authenticated)
    private static boolean serverAuthorizationsReceived;

    private static void updateLoggedIn() {
        Object userId = getUserId();
        if (LogoutUserId.isLogoutUserIdOrNull(userId)) {
            serverAuthorizationsReceived = false;
            FXLoggedIn.setLoggedIn(false);
        } else {
            FXLoggedIn.setLoggedIn(serverAuthorizationsReceived);
            if (!serverAuthorizationsReceived)
                FXAuthorizationsChanged.runOnAuthorizationsChanged(() -> {
                    serverAuthorizationsReceived = true;
                    updateLoggedIn();
                });
        }
    }

    static {
        FXInit.init();
    }

}
