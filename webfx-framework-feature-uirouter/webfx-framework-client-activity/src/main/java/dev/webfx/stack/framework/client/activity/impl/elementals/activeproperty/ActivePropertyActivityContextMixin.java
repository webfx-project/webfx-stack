package dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty;

import javafx.beans.value.ObservableValue;
import dev.webfx.stack.framework.client.activity.ActivityContextMixin;

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
