package dev.webfx.stack.session.state.client.fx;

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
        userPrincipalProperty().setValue(userPrincipal);
    }

    public static Object getUserPrincipal() {
        return userPrincipalProperty().getValue();
    }

}
