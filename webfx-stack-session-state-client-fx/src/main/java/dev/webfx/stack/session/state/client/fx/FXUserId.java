package dev.webfx.stack.session.state.client.fx;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.session.state.client.ClientSideStateSession;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXUserId {

    private final static ObjectProperty<Object> userIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Console.log("FxUserId = " + get());
            ClientSideStateSession.getInstance().changeUserId(get().toString(), true, false);
        }
    };

    public static ObjectProperty<Object> userIdProperty() {
        return userIdProperty;
    }

    public static Object getUserId() {
        return userIdProperty.get();
    }

    public static void setUserId(Object userId) {
        if (!Objects.equals(userId, getUserId()))
            userIdProperty.set(userId);
    }

    static {
        FXInit.init();
    }

}
