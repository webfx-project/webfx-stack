package dev.webfx.stack.ui.action;

import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * @author Bruno Salmon
 */
public interface ActionFactory extends StandardActionKeys {

    ActionBuilder newActionBuilder(Object actionKey);

    default Action newAction(Object actionKey) {
        return newAction(actionKey, (EventHandler<ActionEvent>) null);
    }

    default Action newAction(Object actionKey, Object graphicUrlOrJson) {
        return newAction(actionKey, graphicUrlOrJson, (EventHandler<ActionEvent>) null);
    }

    default Action newAction(Object actionKey, EventHandler<ActionEvent> actionHandler) {
        return newAuthAction(actionKey, actionHandler, null, false);
    }

    default Action newAction(Object actionKey, Object graphicUrlOrJson, EventHandler<ActionEvent> actionHandler) {
        return newAuthAction(actionKey, graphicUrlOrJson, actionHandler, null, false);
    }

    default Action newAuthAction(Object actionKey, ObservableBooleanValue authorizedProperty, boolean hideWhenUnauthorized) {
        return newAuthAction(actionKey, (EventHandler<ActionEvent>) null, authorizedProperty, hideWhenUnauthorized);
    }

    default Action newAuthAction(Object actionKey, Object graphicUrlOrJson, ObservableBooleanValue authorizedProperty,  boolean hideWhenUnauthorized) {
        return newAuthAction(actionKey, graphicUrlOrJson, null, authorizedProperty, hideWhenUnauthorized);
    }

    default Action newAuthAction(Object actionKey, EventHandler<ActionEvent> actionHandler, ObservableBooleanValue authorizedProperty,  boolean hideWhenUnauthorized) {
        return newActionBuilder(actionKey).setActionHandler(actionHandler).setAuthorizedProperty(authorizedProperty).setHiddenWhenDisabled(hideWhenUnauthorized).build();
    }

    default Action newAuthAction(Object actionKey, Object graphicUrlOrJson, EventHandler<ActionEvent> actionHandler, ObservableBooleanValue authorizedProperty,  boolean hideWhenUnauthorized) {
        return newActionBuilder(actionKey).setGraphicUrlOrJson(graphicUrlOrJson).setActionHandler(actionHandler).setAuthorizedProperty(authorizedProperty).setHiddenWhenDisabled(hideWhenUnauthorized).build();
    }

    // Same API but with Runnable

    default Action newAction(Object actionKey, Runnable actionHandler) {
        return newAction(actionKey, e -> actionHandler.run());
    }

    default Action newAction(Object actionKey, Object graphicUrlOrJson, Runnable actionHandler) {
        return newAction(actionKey, graphicUrlOrJson, e -> actionHandler.run());
    }

    default Action newAuthAction(Object actionKey, Runnable actionHandler, ObservableBooleanValue authorizedProperty, boolean hideWhenUnauthorized) {
        return newAuthAction(actionKey, e -> actionHandler.run(), authorizedProperty, hideWhenUnauthorized);
    }

    // Standard actions factories

    default Action newOkAction(Runnable handler) {
        return newAction(OK_ACTION_KEY, handler);
    }

    default Action newCancelAction(Runnable handler) {
        return newAction(CANCEL_ACTION_KEY, handler);
    }

    default Action newSaveAction(Runnable handler) {
        return newAction(SAVE_ACTION_KEY, handler);
    }

    default Action newRevertAction(Runnable handler) {
        return newAction(REVERT_ACTION_KEY, handler);
    }

    default Action newAddAction(Runnable handler) {
        return newAction(ADD_ACTION_KEY, handler);
    }

    default Action newRemoveAction(Runnable handler) {
        return newAction(REMOVE_ACTION_KEY, handler);
    }

}
