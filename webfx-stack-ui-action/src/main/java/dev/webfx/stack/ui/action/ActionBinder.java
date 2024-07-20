package dev.webfx.stack.ui.action;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.kit.util.properties.ObservableLists;
import dev.webfx.platform.util.function.Converter;
import dev.webfx.stack.ui.action.impl.WritableAction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public final class ActionBinder {

    public static <T extends ButtonBase> T bindButtonToAction(T button, Action action) {
        bindLabeledToAction(button, action);
        button.setOnAction(action);
        return button;
    }

    public static <T extends MenuItem> T bindMenuItemToAction(T menuItem, Action action) {
        menuItem.textProperty().bind(action.textProperty());
        bindGraphicProperties(menuItem.graphicProperty(), action.graphicProperty());
        menuItem.disableProperty().bind(action.disabledProperty());
        menuItem.visibleProperty().bind(action.visibleProperty());
        menuItem.setOnAction(action);
        return menuItem;
    }

    private static <T extends Labeled> T bindLabeledToAction(T labeled, Action action) {
        labeled.textProperty().bind(action.textProperty());
        bindGraphicProperties(labeled.graphicProperty(), action.graphicProperty());
        bindNodeToAction(labeled, action, false);
        return labeled;
    }

    private static void bindGraphicProperties(ObjectProperty<Node> dstGraphicProperty, ObservableObjectValue<Node> srcGraphicProperty) {
        // Needs to make a copy of the graphic in case it is used in several places (JavaFX nodes must be unique instances in the scene graph)
        FXProperties.runNowAndOnPropertiesChange(p -> dstGraphicProperty.setValue(copyGraphic(srcGraphicProperty.getValue())), srcGraphicProperty);
    }

    private static Node copyGraphic(Node graphic) {
        // Handling only ImageView for now
        if (graphic instanceof ImageView) {
            ImageView imageView = (ImageView) graphic;
            ImageView copy = new ImageView();
            copy.imageProperty().bind(imageView.imageProperty());
            copy.fitWidthProperty().bind(imageView.fitWidthProperty());
            copy.fitHeightProperty().bind(imageView.fitHeightProperty());
            return copy;
        }
        return graphic;
    }

    public static Node getAndBindActionIcon(Action action) {
        return bindNodeToAction(action.getGraphic(), action, true);
    }

    private static <T extends Node> T bindNodeToAction(T node, Action action, boolean setOnMouseClicked) {
        node.disableProperty().bind(action.disabledProperty());
        node.visibleProperty().bind(action.visibleProperty());
        // Automatically removing the node from layout if not visible
        node.managedProperty().bind(node.visibleProperty());
        if (setOnMouseClicked)
            node.setOnMouseClicked(e -> action.handle(new ActionEvent(e.getSource(), e.getTarget())));
        return node;
    }

    public static void bindWritableActionToAction(WritableAction writableAction, Action action) {
        writableAction.writableTextProperty().bind(action.textProperty());
        writableAction.writableGraphicProperty().bind(action.graphicProperty());
        writableAction.writableDisabledProperty().bind(action.disabledProperty());
        writableAction.writableVisibleProperty().bind(action.visibleProperty());
    }

    public static <P extends Pane> P bindChildrenToVisibleActions(P parent, Collection<Action> actions, Converter<Action, Node> nodeFactory) {
        ActionGroup actionGroup = new ActionGroupBuilder().setActions(actions).build();
        return bindChildrenToActionGroup(parent, actionGroup, nodeFactory);
    }

    public static <P extends Pane> P bindChildrenToActionGroup(P parent, ActionGroup actionGroup, Converter<Action, Node> nodeFactory) {
        bindChildrenToActionGroup(parent.getChildren(), actionGroup, nodeFactory);
        return parent;
    }

    public static <T extends Node> ObservableList<T> bindChildrenToActionGroup(ObservableList<T> children, ActionGroup actionGroup, Converter<Action, T> nodeFactory) {
        ObservableLists.bindConverted(children, actionGroup.getVisibleActions(), nodeFactory);
        return children;
    }
}
