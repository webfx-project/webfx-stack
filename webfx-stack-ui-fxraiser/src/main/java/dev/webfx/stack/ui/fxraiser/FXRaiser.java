package dev.webfx.stack.ui.fxraiser;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.ui.fxraiser.impl.DefaultFXValueRaiser;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public class FXRaiser {

    private static FXValueRaiser fxValueRaiserInstance = new DefaultFXValueRaiser();

    public static FXValueRaiser getFxValueRaiserInstance() {
        return fxValueRaiserInstance;
    }

    public static void setFxValueRaiserInstance(FXValueRaiser fxValueRaiserInstance) {
        FXRaiser.fxValueRaiserInstance = fxValueRaiserInstance;
    }

    public static ObservableStringValue raiseToStringProperty(Object value, Object... args) {
        return raiseToStringProperty(value, null, args);
    }

    public static ObservableStringValue raiseToStringProperty(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return (ObservableStringValue) raiseToProperty(value, String.class, fxValueRaiser, args);
    }

    public static ObservableBooleanValue raiseToBooleanProperty(Object value, Object... args) {
        return raiseToBooleanProperty(value, null, args);
    }

    public static ObservableBooleanValue raiseToBooleanProperty(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return (ObservableBooleanValue) raiseToProperty(value, Boolean.class, fxValueRaiser, args);
    }

    public static ObservableDoubleValue raiseToDoubleProperty(Object value, Object... args) {
        return raiseToDoubleProperty(value, null, args);
    }

    public static ObservableDoubleValue raiseToDoubleProperty(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return (ObservableDoubleValue) raiseToProperty(value, Number.class, fxValueRaiser, Double.class, args);
    }

    public static ObservableIntegerValue raiseToIntegerProperty(Object value, Object... args) {
        return raiseToIntegerProperty(value, null, args);
    }

    public static ObservableIntegerValue raiseToIntegerProperty(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return (ObservableIntegerValue) raiseToProperty(value, Number.class, fxValueRaiser, Integer.class, args);
    }

    public static ObservableValue<Node> raiseToNodeProperty(Object value, Object... args) {
        return raiseToNodeProperty(value, null, args);
    }

    public static ObservableValue<Node> raiseToNodeProperty(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToProperty(value, Node.class, fxValueRaiser, args);
    }

    public static <T> ObservableValue<T> raiseToProperty(Object value, Class<T> raisedClass, Object... args) {
        return raiseToProperty(value, raisedClass, null, args);
    }

    public static <T> ObservableValue<T> raiseToProperty(Object value, Class<T> raisedClass, FXValueRaiser fxValueRaiser, Object... args) {
        Property<T> raisedProperty;
        if (raisedClass.equals(String.class))
            raisedProperty = (Property<T>) new SimpleStringProperty();
        else if (raisedClass.equals(Boolean.class))
            raisedProperty = (Property<T>) new SimpleBooleanProperty();
        else if (raisedClass.equals(Number.class)) {
            raisedClass = (Class) args[0];
            args = (Object[]) args[1];
            if (raisedClass.equals(Double.class))
                raisedProperty = (Property<T>) new SimpleDoubleProperty();
            else if (raisedClass.equals(Integer.class))
                raisedProperty = (Property<T>) new SimpleIntegerProperty();
            else
                raisedProperty = null;
        } else
            raisedProperty = new SimpleObjectProperty<>();
        Collection<ObservableValue> dependencies = new ArrayList<>();
        addIfObservableValue(value, dependencies);
        for (Object arg : args)
            addIfObservableValue(arg, dependencies);
        Class<T> finalRaisedClass = raisedClass;
        Object[] finalArgs = args;
        FXProperties.runNowAndOnPropertiesChange(() -> {
            Object[] rawArgs = Arrays.stream(finalArgs).map(FXRaiser::getRawValue).toArray();
            T raisedValue = raiseToObject(value, finalRaisedClass, fxValueRaiser, rawArgs);
            raisedProperty.setValue(raisedValue);
        }, dependencies);
        return raisedProperty;
    }

    public static String raiseToString(Object value, Object... args) {
        return raiseToString(value, null, args);
    }

    public static String raiseToString(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToObject(value, String.class, fxValueRaiser, args);
    }

    public static Boolean raiseToBoolean(Object value, Object... args) {
        return raiseToBoolean(value, null, args);
    }

    public static Boolean raiseToBoolean(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToObject(value, Boolean.class, fxValueRaiser, args);
    }

    public static Double raiseToDouble(Object value, Object... args) {
        return raiseToDouble(value, null, args);
    }

    public static Double raiseToDouble(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToObject(value, Double.class, fxValueRaiser, args);
    }

    public static Integer raiseToInteger(Object value, Object... args) {
        return raiseToInteger(value, null, args);
    }

    public static Integer raiseToInteger(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToObject(value, Integer.class, fxValueRaiser);
    }

    public static Node raiseToNode(Object value, Object... args) {
        return raiseToNode(value, null, args);
    }

    public static Node raiseToNode(Object value, FXValueRaiser fxValueRaiser, Object... args) {
        return raiseToObject(value, Node.class, fxValueRaiser, args);
    }

    public static <T> T raiseToObject(Object value, Class<T> raisedClass, Object... args) {
        return raiseToObject(value, raisedClass, null, args);
    }

    public static <T> T raiseToObject(Object value, Class<T> raisedClass, FXValueRaiser fxValueRaiser, Object... args) {
        if (fxValueRaiser == null)
            fxValueRaiser = getFxValueRaiserInstance();
        return fxValueRaiser.raiseValue(value, raisedClass, args);
    }

    private static void addIfObservableValue(Object value, Collection<ObservableValue> observableValues) {
        if (value instanceof ObservableValue)
            observableValues.add((ObservableValue) value);
    }

    private static Object getRawValue(Object value) {
        return value instanceof ObservableValue ? ((ObservableValue<?>) value).getValue() : value;
    }

}
