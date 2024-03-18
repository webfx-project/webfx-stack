package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXServerSessionId {

    private final static ObjectProperty<Object> serverSessionIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Object serverSessionId = get();
            Console.log("FxServerSessionId = " + serverSessionId);
            ClientSideStateSession.getInstance().changeServerSessionId(serverSessionId.toString(), true, false);
        }
    };

    public static ReadOnlyObjectProperty<Object> serverSessionIdProperty() {
        return serverSessionIdProperty;
    }

    public static Object getServerSessionId() {
        return serverSessionIdProperty.get();
    }

    static void setServerSessionId(Object serverSessionId) {
        FXProperties.setIfNotEquals(serverSessionIdProperty, serverSessionId);
    }

    static {
        FXInit.init();
    }

}
