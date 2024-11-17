package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXUserPrincipal {

    private final static ObjectProperty<Object> userPrincipalProperty = new SimpleObjectProperty<>();

    public static Property<Object> userPrincipalProperty() {
        return userPrincipalProperty;
    }

    public static void setUserPrincipal(Object userPrincipal) {
        FXProperties.setIfNotEquals(userPrincipalProperty, userPrincipal);
    }

    public static Object getUserPrincipal() {
        return userPrincipalProperty().getValue();
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

}
