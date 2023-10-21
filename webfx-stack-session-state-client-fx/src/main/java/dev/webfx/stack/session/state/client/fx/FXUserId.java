package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXUserId {

    private final static ObjectProperty<Object> userIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Object userId = get();
            Console.log("FxUserId = " + userId);
            ClientSideStateSession.getInstance().changeUserId(userId, false, false);
        }
    };

    public static ObjectProperty<Object> userIdProperty() {
        return userIdProperty;
    }

    public static Object getUserId() {
        return userIdProperty.get();
    }

    public static void setUserId(Object userId) {
        FXProperties.setIfNotEquals(userIdProperty, userId);
    }

    static {
        FXInit.init();
    }

}
