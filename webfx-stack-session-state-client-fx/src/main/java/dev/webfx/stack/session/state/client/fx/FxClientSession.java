package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FxClientSession {

    private final static ObjectProperty<Session> clientSessionProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Session session = get();
            String localSessionId = session == null ? null : session.id();
            String sessionId = session.get("sessionId");
            Console.log("FxClientSession: localId = " + localSessionId + " - sessionId = " + sessionId);
            ClientSideStateSession.getInstance().setClientSession(session);
        }
    };

    public static ObjectProperty<Session> clientSessionProperty() {
        return clientSessionProperty;
    }

    public static Session getClientSession() {
        return clientSessionProperty.get();
    }

    public static void setClientSession(Session clientSession) {
        clientSessionProperty.set(clientSession);
    }

    static {
        FxInit.init();
    }

}
