package dev.webfx.stack.routing.activity.impl.elementals.activeproperty;

import javafx.beans.value.ObservableBooleanValue;

/**
 * @author Bruno Salmon
 */
public interface HasActiveProperty {

    ObservableBooleanValue activeProperty();

    default boolean isActive() {
        return activeProperty().getValue();
    }

}
