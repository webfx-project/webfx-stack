package dev.webfx.stack.routing.activity.impl.elementals.activeproperty.impl;

import dev.webfx.stack.routing.activity.impl.ActivityBase;
import javafx.beans.property.Property;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivity;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContext;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public class ActivePropertyActivityBase
        <C extends ActivePropertyActivityContext<C>>

        extends ActivityBase<C>
        implements ActivePropertyActivity<C>,
        ActivePropertyActivityContextMixin<C> {

    @Override
    protected void setActive(boolean active) {
        super.setActive(active);
        ((Property<Boolean>) activeProperty()).setValue(active);
    }

}
