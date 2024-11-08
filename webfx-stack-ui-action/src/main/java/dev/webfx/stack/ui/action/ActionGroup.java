package dev.webfx.stack.ui.action;

import dev.webfx.stack.ui.action.impl.ActionGroupImpl;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public interface ActionGroup extends Action {

    Collection<Action> getActions();

    ObservableList<Action> getVisibleActions();

    boolean hasSeparators();

    static ActionGroup create(Collection<Action> actions, ObservableStringValue textProperty, ObservableValue<Supplier<Node>> graphicFactoryProperty, ObservableBooleanValue disabledProperty, ObservableBooleanValue visibleProperty, boolean hasSeparators, EventHandler<ActionEvent> actionHandler) {
        return new ActionGroupImpl(actions, textProperty, graphicFactoryProperty, disabledProperty, visibleProperty, hasSeparators, actionHandler);
    }

}
