package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FxClientUserId {

    private final static ObjectProperty<Object> clientUserIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Console.log("FxClientUserId = " + get());
            ClientSideStateSession.getInstance().changeUserId(get().toString(), true, false);
        }
    };

    public static ObjectProperty<Object> clientUserIdProperty() {
        return clientUserIdProperty;
    }

    public static Object getClientUserId() {
        return clientUserIdProperty.get();
    }

    public static void setClientUserId(Object clientUserId) {
        if (!Objects.equals(clientUserId, getClientUserId()))
            clientUserIdProperty.set(clientUserId);
    }

    static {
        FxInit.init();
    }

}
