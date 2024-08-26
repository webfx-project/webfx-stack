package dev.webfx.stack.authn.login.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXLoginContext {

    private static final ObjectProperty<Object> loginContextProperty = new SimpleObjectProperty<>();

    public static ObjectProperty<Object> loginContextProperty() {
        return loginContextProperty;
    }

    public static Object getLoginContext() {
        return loginContextProperty.get();
    }

    public static void setLoginContext(Object loginContext) {
        loginContextProperty.set(loginContext);
    }

}
