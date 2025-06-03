package dev.webfx.stack.ui.action;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.ui.json.JsonImageView;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public class ActionBuilder {

    private Object actionKey;

    private ObservableStringValue textProperty;
    private String text;
    private Object i18nKey;

    private ObservableValue<Supplier<Node>> graphicFactoryProperty;
    private Supplier<Node> graphicFactory;
    private Object graphicUrlOrJson;

    private ObservableBooleanValue disabledProperty;
    private ObservableBooleanValue visibleProperty;

    private boolean hiddenWhenDisabled = true;

    private boolean authRequired;

    private ObservableBooleanValue authorizedProperty;

    private EventHandler<ActionEvent> actionHandler;

    private Object userData;

    private ActionBuilderRegistry registry;

    public ActionBuilder() {
    }

    public ActionBuilder(Object actionKey) {
        this.actionKey = actionKey;
    }

    public ActionBuilder(Action action) {
        setTextProperty(action.textProperty());
        setText(action.getText());
        setGraphicFactoryProperty(action.graphicFactoryProperty());
        setGraphicFactory(action.getGraphicFactory());
        setDisabledProperty(action.disabledProperty());
        setVisibleProperty(action.visibleProperty());
        setActionHandler(action);
        setUserData(action.getUserData());
        //setHiddenWhenDisabled(visibleProperty == disabledProperty || visibleProperty instanceof BooleanBinding && ((BooleanBinding) visibleProperty).getDependencies().contains(disabledProperty));
    }

    public Object getActionKey() {
        return actionKey;
    }

    public ActionBuilder setActionKey(Object actionKey) {
        this.actionKey = actionKey;
        return this;
    }

    public ObservableStringValue getTextProperty() {
        return textProperty;
    }

    public ActionBuilder setTextProperty(ObservableStringValue textProperty) {
        this.textProperty = textProperty;
        return this;
    }

    public String getText() {
        return text;
    }

    public ActionBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public Object getI18nKey() {
        return i18nKey;
    }

    public ActionBuilder setI18nKey(Object i18nKey) {
        this.i18nKey = i18nKey;
        return this;
    }

    public ObservableValue<Supplier<Node>> getGraphicFactoryProperty() {
        return graphicFactoryProperty;
    }

    public ActionBuilder setGraphicFactoryProperty(ObservableValue<Supplier<Node>> graphicFactoryProperty) {
        this.graphicFactoryProperty = graphicFactoryProperty;
        return this;
    }

    public Supplier<Node> getGraphicFactory() {
        return graphicFactory;
    }

    public ActionBuilder setGraphicFactory(Supplier<Node> graphicFactory) {
        this.graphicFactory = graphicFactory;
        return this;
    }

    public Object getGraphicUrlOrJson() {
        return graphicUrlOrJson;
    }

    public ActionBuilder setGraphicUrlOrJson(Object graphicUrlOrJson) {
        this.graphicUrlOrJson = graphicUrlOrJson;
        return this;
    }

    public ObservableBooleanValue getDisabledProperty() {
        return disabledProperty;
    }

    public ActionBuilder setDisabledProperty(ObservableBooleanValue disabledProperty) {
        this.disabledProperty = disabledProperty;
        return this;
    }

    public ObservableBooleanValue getVisibleProperty() {
        return visibleProperty;
    }

    public ActionBuilder setVisibleProperty(ObservableBooleanValue visibleProperty) {
        this.visibleProperty = visibleProperty;
        return this;
    }

    public boolean isHiddenWhenDisabled() {
        return hiddenWhenDisabled;
    }

    public ActionBuilder setHiddenWhenDisabled(boolean hiddenWhenDisabled) {
        this.hiddenWhenDisabled = hiddenWhenDisabled;
        return this;
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    public ActionBuilder setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
        return this;
    }

    public ObservableBooleanValue getAuthorizedProperty() {
        return authorizedProperty;
    }

    public ActionBuilder setAuthorizedProperty(ObservableBooleanValue authorizedProperty) {
        this.authorizedProperty = authorizedProperty;
        return this;
    }

    public EventHandler<ActionEvent> getActionHandler() {
        return actionHandler;
    }

    public ActionBuilder setActionHandler(EventHandler<ActionEvent> actionHandler) {
        this.actionHandler = actionHandler;
        return this;
    }

    public ActionBuilder setActionHandler(Runnable actionHandler) {
        return setActionHandler(e -> actionHandler.run());
    }

    public Object getUserData() {
        return userData;
    }

    public ActionBuilder setUserData(Object userData) {
        this.userData = userData;
        return this;
    }

    public ActionBuilder setRegistry(ActionBuilderRegistry registry) {
        this.registry = registry;
        return this;
    }

    public ActionBuilder register() {
        (registry != null ? registry : ActionBuilderRegistry.get()).registerActionBuilder(this);
        return this;
    }

    public ActionBuilder duplicate() {
        return newActionBuilder(actionKey)
                .setTextProperty(textProperty)
                .setText(text)
                .setI18nKey(i18nKey)
                .setGraphicFactoryProperty(graphicFactoryProperty)
                .setGraphicFactory(graphicFactory)
                .setGraphicUrlOrJson(graphicUrlOrJson)
                .setDisabledProperty(disabledProperty)
                .setVisibleProperty(visibleProperty)
                .setHiddenWhenDisabled(hiddenWhenDisabled)
                .setAuthRequired(authRequired)
                .setAuthorizedProperty(authorizedProperty)
                .setActionHandler(actionHandler)
                ;
    }

    ActionBuilder newActionBuilder(Object actionKey) {
        return new ActionBuilder(actionKey);
    }

    public ActionBuilder removeText() {
        textProperty = null;
        text = null;
        i18nKey = null;
        return this;
    }

    public Action build() {
        completePropertiesForBuild();
        Action action = Action.create(textProperty, graphicFactoryProperty, disabledProperty, visibleProperty, actionHandler);
        action.setUserData(userData);
        return action;
    }

    void completePropertiesForBuild() {
        completeTextProperty();
        completeGraphicProperty();
        completeDisabledProperty();
        completeVisibleProperty();
    }

    private void completeTextProperty() {
        if (textProperty == null) {
            if (i18nKey != null)
                textProperty = I18n.i18nTextProperty(i18nKey);
            else
                textProperty = new SimpleStringProperty(text);
        }
    }

    private void completeGraphicProperty() {
        if (graphicFactoryProperty == null) {
            if (graphicFactory == null && graphicUrlOrJson != null)
                graphicFactory = () -> JsonImageView.createImageView(graphicUrlOrJson);
            if (graphicFactory != null || i18nKey == null)
                graphicFactoryProperty = new SimpleObjectProperty<>(graphicFactory);
            else
                graphicFactoryProperty = FXProperties.compute(I18n.dictionaryProperty(), dictionary ->
                    () -> I18n.getI18nGraphic(i18nKey));
        }
    }

    private void completeDisabledProperty() {
        if (disabledProperty == null) {
            if (authorizedProperty != null) {
                disabledProperty = BooleanExpression.booleanExpression(authorizedProperty).not();
                if (hiddenWhenDisabled && visibleProperty == null)
                    visibleProperty = authorizedProperty;
            } else
                disabledProperty = new SimpleBooleanProperty(authRequired);
        }
    }

    private void completeVisibleProperty() {
        if (visibleProperty == null) {
            if (hiddenWhenDisabled)
                visibleProperty = BooleanExpression.booleanExpression(disabledProperty).not();
            else
                visibleProperty = new SimpleBooleanProperty(true);
        } else if (hiddenWhenDisabled) {
            visibleProperty = BooleanExpression.booleanExpression(visibleProperty).and(BooleanExpression.booleanExpression(disabledProperty).not());
        }
    }
}
