package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXRunId {

    private final static ObjectProperty<Object> runIdProperty = FXProperties.newObjectProperty(runId -> {
        Console.log("FxRunId = " +runId);
        ClientSideStateSession.getInstance().changeRunId(runId.toString(), true, false);
    });

    public static ReadOnlyObjectProperty<Object> runIdProperty() {
        return runIdProperty;
    }

    public static Object getRunId() {
        return runIdProperty.get();
    }

    // TODO remove that public visibility
    public static void setRunId(Object runId) {
        FXProperties.setIfNotEquals(runIdProperty, runId);
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

}
