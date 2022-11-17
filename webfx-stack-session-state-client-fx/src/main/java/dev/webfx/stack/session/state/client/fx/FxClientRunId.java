package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FxClientRunId {

    private final static ObjectProperty<Object> clientRunIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Console.log("FxClientRunId = " + get());
            ClientSideStateSession.getInstance().changeRunId(get().toString(), true, false);
        }
    };

    public static ObjectProperty<Object> clientRunIdProperty() {
        return clientRunIdProperty;
    }

    public static Object getClientRunId() {
        return clientRunIdProperty.get();
    }

    public static void setClientRunId(Object clientRunId) {
        if (!Objects.areEquals(clientRunId, getClientRunId()))
            clientRunIdProperty.set(clientRunId);
    }

    static {
        FxInit.init();
    }

}
