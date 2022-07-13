package dev.webfx.stack.routing.uirouter.uisession.impl;

import dev.webfx.stack.routing.uirouter.uisession.UiSession;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.Objects;


/**
 * @author Bruno Salmon
 */
public final class UiSessionImpl implements UiSession {

    private final Property<Object> userPrincipalProperty = new SimpleObjectProperty<>();
    private final ObservableBooleanValue loggedInProperty = BooleanExpression.booleanExpression(FXProperties.compute(userPrincipalProperty, Objects::nonNull));

    private final static UiSession INSTANCE = new UiSessionImpl();

    public static UiSession getInstance() {
        return INSTANCE;
    }

    private UiSessionImpl() {
    }

    @Override
    public Property<Object> userPrincipalProperty() {
        return userPrincipalProperty;
    }

    @Override
    public ObservableBooleanValue loggedInProperty() {
        return loggedInProperty;
    }

}
