package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXRunId {

    private final static ObjectProperty<Object> runIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Console.log("FxRunId = " + get());
            ClientSideStateSession.getInstance().changeRunId(get().toString(), true, false);
        }
    };

    public static ReadOnlyObjectProperty<Object> runIdProperty() {
        return runIdProperty;
    }

    public static Object getRunId() {
        return runIdProperty.get();
    }

    // TODO remove that public visibility
    public static void setRunId(Object runId) {
        if (!Objects.areEquals(runId, getRunId()))
            runIdProperty.set(runId);
    }

    static {
        FXInit.init();
    }

}
