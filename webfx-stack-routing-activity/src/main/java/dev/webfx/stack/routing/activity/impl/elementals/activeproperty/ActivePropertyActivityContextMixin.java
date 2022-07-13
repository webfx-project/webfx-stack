package dev.webfx.stack.routing.activity.impl.elementals.activeproperty;

import dev.webfx.stack.routing.activity.ActivityContextMixin;
import javafx.beans.value.ObservableValue;

/**
 * @author Bruno Salmon
 */
public interface ActivePropertyActivityContextMixin
        <C extends ActivePropertyActivityContext<C>>

        extends ActivityContextMixin<C>,
        ActivePropertyActivityContext<C> {

    @Override
    default ObservableValue<Boolean> activeProperty() {
        return getActivityContext().activeProperty();
    }

}
