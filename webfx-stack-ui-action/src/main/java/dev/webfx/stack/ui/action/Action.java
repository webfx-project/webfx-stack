package dev.webfx.stack.ui.action;

import dev.webfx.stack.ui.action.impl.ReadOnlyAction;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * An action compatible with standard JavaFX API (ex: can be passed to Button.setOnAction()) but enriched with graphical
 * properties (ie text, graphic, disabled and visible properties). The ActionBinder utility class can be used to help
 * binding graphical components (such as buttons) to actions. The ActionBuilder utility class can be used to
 *
 * @author Bruno Salmon
 */
public interface Action extends EventHandler<ActionEvent> {

    ObservableStringValue textProperty();
    default String getText() {
        return textProperty().get();
    }

    ObservableValue<Supplier<Node>> graphicFactoryProperty(); // TODO: should it be rather Supplier<ObservableValue<Node>>?
    default Supplier<Node> getGraphicFactory() {
        return graphicFactoryProperty().getValue();
    }

    default Node createGraphic() {
        Supplier<Node> graphicFactory = getGraphicFactory();
        return graphicFactory == null ? null : graphicFactory.get();
    }

    ObservableBooleanValue disabledProperty();
    default boolean isDisabled() {
        return disabledProperty().get();
    }

    ObservableBooleanValue visibleProperty();
    default boolean isVisible() {
        return visibleProperty().get();
    }

    void setUserData(Object userData);

    Object getUserData();

    static Action create(ObservableStringValue textProperty, ObservableValue<Supplier<Node>> graphicFactoryProperty, ObservableBooleanValue disabledProperty, ObservableBooleanValue visibleProperty, EventHandler<ActionEvent> actionHandler) {
        return new ReadOnlyAction(textProperty, graphicFactoryProperty, disabledProperty, visibleProperty, actionHandler);
    }
}
