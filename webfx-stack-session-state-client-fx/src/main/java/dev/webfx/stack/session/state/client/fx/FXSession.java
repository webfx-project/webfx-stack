package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXSession {

    private final static ObjectProperty<Session> sessionProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Session session = get();
            String localSessionId = session == null ? null : session.id();
            Console.log("FxSession: localId = " + localSessionId);
            ClientSideStateSession.getInstance().setClientSession(session);
        }
    };

    public static ReadOnlyObjectProperty<Session> sessionProperty() {
        return sessionProperty;
    }

    public static Session getSession() {
        return sessionProperty.get();
    }

    static void setSession(Session session) {
        sessionProperty.set(session);
    }

    static {
        FXInit.init();
    }

}
