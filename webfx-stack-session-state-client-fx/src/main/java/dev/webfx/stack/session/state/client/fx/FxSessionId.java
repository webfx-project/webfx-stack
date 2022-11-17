package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FxSessionId {

    private final static ObjectProperty<Object> clientSessionIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Object sessionId = get();
            Console.log("FxSessionId = " + sessionId);
            ClientSideStateSession.getInstance().changeSessionId(sessionId.toString(), true, false);
        }
    };

    public static ObjectProperty<Object> clientSessionIdProperty() {
        return clientSessionIdProperty;
    }

    public static Object getClientSessionId() {
        return clientSessionIdProperty.get();
    }

    public static void setClientSessionId(Object sessionId) {
        if (!Objects.equals(sessionId, getClientSessionId()))
            clientSessionIdProperty.set(sessionId);
    }

    static {
        FxInit.init();
    }

}
