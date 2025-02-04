package dev.webfx.stack.ui.action.impl;

import dev.webfx.stack.ui.action.Action;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * A read-only action where properties are observable values.
 *
 * @author Bruno Salmon
 */
public class ReadOnlyAction implements Action {

    public ReadOnlyAction(ObservableStringValue textProperty, ObservableValue<Supplier<Node>> graphicFactoryProperty, ObservableBooleanValue disabledProperty, ObservableBooleanValue visibleProperty, EventHandler<ActionEvent> actionHandler) {
        this.textProperty = textProperty;
        this.graphicFactoryProperty = graphicFactoryProperty;
        this.disabledProperty = disabledProperty;
        this.visibleProperty = visibleProperty;
        this.actionHandler = actionHandler;
    }

    private final ObservableStringValue textProperty;
    @Override
    public ObservableStringValue textProperty() {
        return textProperty;
    }

    private final ObservableValue<Supplier<Node>> graphicFactoryProperty;
    @Override
    public ObservableValue<Supplier<Node>> graphicFactoryProperty() {
        return graphicFactoryProperty;
    }

    private final ObservableBooleanValue disabledProperty;
    @Override
    public ObservableBooleanValue disabledProperty() {
        return disabledProperty;
    }

    private final ObservableBooleanValue visibleProperty;
    @Override
    public ObservableBooleanValue visibleProperty() {
        return visibleProperty;
    }

    private Object userData;

    @Override
    public Object getUserData() {
        return userData;
    }

    @Override
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    private final EventHandler<ActionEvent> actionHandler;
    @Override
    public void handle(ActionEvent event) {
        // Calling the action handler unless the action is disabled
        if (actionHandler != null && !isDisabled())
            actionHandler.handle(event);
    }
}
