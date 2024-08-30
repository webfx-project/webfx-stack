package dev.webfx.stack.routing.activity.impl.elementals.activeproperty;

import dev.webfx.stack.routing.activity.ActivityContextMixin;
import javafx.beans.value.ObservableBooleanValue;

/**
 * @author Bruno Salmon
 */
public interface ActivePropertyActivityContextMixin
        <C extends ActivePropertyActivityContext<C>>

        extends ActivityContextMixin<C>,
        ActivePropertyActivityContext<C> {

    @Override
    default ObservableBooleanValue activeProperty() {
        return getActivityContext().activeProperty();
    }

}
