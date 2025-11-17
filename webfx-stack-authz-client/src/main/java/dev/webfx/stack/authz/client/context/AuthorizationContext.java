package dev.webfx.stack.authz.client.context;

import dev.webfx.stack.session.state.client.fx.FXAuthorizationsChanged;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.HashMap;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationContext {

    private static final ObservableMap<String, String> CONTEXT_PROPERTIES = FXCollections.observableMap(new HashMap<>());

    public static ObservableMap<String, String> getContextProperties() {
        return CONTEXT_PROPERTIES;
    }

    public static void setContextProperty(String name, String value) {
        CONTEXT_PROPERTIES.put(name, value);
        FXAuthorizationsChanged.fireAuthorizationsChanged();
    }
}
