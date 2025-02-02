package dev.webfx.stack.ui.action.impl;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.ui.action.Action;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * A writable action where properties (text, graphic, disabled, visible) can be set later (ie after constructor call)
 * either by calling the setters or by binding these properties (ex: writableTextProperty().bind(myTextProperty))
 *
 * @author Bruno Salmon
 */
public class WritableAction extends ReadOnlyAction {

    public WritableAction(Action action, String... writablePropertyNames) {
        this(action, null, writablePropertyNames);
    }

    private WritableAction(Action action, ObservableBooleanValue additionalDisabledProperty, String... writablePropertyNames) {
        this( createStringProperty(action.textProperty(), "text", writablePropertyNames)
            , createObjectProperty(action.graphicFactoryProperty(), "graphicFactory", writablePropertyNames)
            // if additionalDisabledProperty is not null, we force this writable action to be disabled when this additional disabled property is true
            , createOrBooleanProperty(action.disabledProperty(), additionalDisabledProperty, "disabled", writablePropertyNames)
            // if additionalDisabledProperty is not null, we force this writable action to be invisible when this additional disabled property is true
            // Please note that it's necessary to create a new property in this case and just not binding the existing one because the existing one may be (re)bound later by OperationActionRegistry, which would break the additional binding
            , createAndBooleanProperty(action.visibleProperty(), additionalDisabledProperty == null ? null : Bindings.not(additionalDisabledProperty), "visible", writablePropertyNames)
            , action);
    }

    public WritableAction(EventHandler<ActionEvent> actionHandler) {
        this(new SimpleStringProperty(), new SimpleObjectProperty<>(), new SimpleBooleanProperty(true /* disabled until it is bound */), new SimpleBooleanProperty(false /* invisible until it is bound */), actionHandler);
    }

    public WritableAction(ObservableStringValue textProperty, ObservableValue<Supplier<Node>> graphicFactoryProperty, ObservableBooleanValue disabledProperty, ObservableBooleanValue visibleProperty, EventHandler<ActionEvent> actionHandler) {
        super(textProperty, graphicFactoryProperty, disabledProperty, visibleProperty, actionHandler);
    }

    public StringProperty writableTextProperty() {
        return (StringProperty) textProperty();
    }

    public void setText(String text) {
        writableTextProperty().set(text);
    }

    public ObjectProperty<Supplier<Node>> writableGraphicFactoryProperty() {
        return (ObjectProperty<Supplier<Node>>) graphicFactoryProperty();
    }

    public void setGraphicFactory(Supplier<Node> graphicFactory) {
        writableGraphicFactoryProperty().set(graphicFactory);
    }

    public BooleanProperty writableDisabledProperty() {
        return (BooleanProperty) disabledProperty();
    }

    public void setDisabled(boolean disabled) {
        writableDisabledProperty().set(disabled);
    }

    public BooleanProperty writableVisibleProperty() {
        return (BooleanProperty) visibleProperty();
    }

    public void setVisible(boolean visible) {
        writableVisibleProperty().set(visible);
    }

    private static StringProperty createStringProperty(ObservableStringValue readOnlyProperty, String propertyName, String... writablePropertyNames) {
        if (readOnlyProperty instanceof StringProperty)
            return (StringProperty) readOnlyProperty;
        SimpleStringProperty writableProperty = new SimpleStringProperty() {
            @Override
            public void set(String newValue) {
                unbindPropertyIfWritable(this, propertyName, writablePropertyNames);
                super.set(newValue);
            }
        };
        writableProperty.bind(readOnlyProperty);
        return writableProperty;
    }

    private static <T> ObjectProperty<T> createObjectProperty(ObservableValue readOnlyProperty, String propertyName, String... writablePropertyNames) {
        if (readOnlyProperty instanceof ObjectProperty)
            return (ObjectProperty<T>) readOnlyProperty;
        SimpleObjectProperty<T> writableProperty = new SimpleObjectProperty<>() {
            @Override
            public void set(T newValue) {
                unbindPropertyIfWritable(this, propertyName, writablePropertyNames);
                super.set(newValue);
            }
        };
        writableProperty.bind(readOnlyProperty);
        return writableProperty;
    }

    private static ObservableBooleanValue createOrBooleanProperty(ObservableBooleanValue readOnlyProperty, ObservableBooleanValue additionalBooleanProperty, String propertyName, String... writablePropertyNames) {
        BooleanProperty booleanProperty = createBooleanProperty(readOnlyProperty, propertyName, writablePropertyNames);
        return additionalBooleanProperty == null ? booleanProperty : Bindings.or(booleanProperty, additionalBooleanProperty);
    }

    private static ObservableBooleanValue createAndBooleanProperty(ObservableBooleanValue readOnlyProperty, ObservableBooleanValue additionalBooleanProperty, String propertyName, String... writablePropertyNames) {
        BooleanProperty booleanProperty = createBooleanProperty(readOnlyProperty, propertyName, writablePropertyNames);
        return additionalBooleanProperty == null ? booleanProperty : Bindings.and(booleanProperty, additionalBooleanProperty);
    }

    private static BooleanProperty createBooleanProperty(ObservableBooleanValue readOnlyProperty, String propertyName, String... writablePropertyNames) {
        if (readOnlyProperty instanceof BooleanProperty)
            return (BooleanProperty) readOnlyProperty;
        SimpleBooleanProperty writableProperty = new SimpleBooleanProperty() {
            @Override
            public void set(boolean newValue) {
                unbindPropertyIfWritable(this, propertyName, writablePropertyNames);
                super.set(newValue);
            }
        };
        writableProperty.bind(readOnlyProperty);
        return writableProperty;
    }

    private static void unbindPropertyIfWritable(Property property, String propertyName, String... writablePropertyNames) {
        if (Arrays.contains(writablePropertyNames, propertyName) || Arrays.contains(writablePropertyNames, "*"))
            property.unbind();
    }

    public static WritableAction overrideActionWithAdditionalDisabledProperty(Action action, ObservableBooleanValue additionalDisabledProperty) {
        return new WritableAction(action, additionalDisabledProperty);
    }
}
