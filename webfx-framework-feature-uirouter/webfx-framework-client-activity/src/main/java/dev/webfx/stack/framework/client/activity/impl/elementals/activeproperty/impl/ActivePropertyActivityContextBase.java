package dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.impl;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.ActivePropertyActivityContext;
import dev.webfx.stack.framework.client.activity.ActivityContext;
import dev.webfx.stack.framework.client.activity.ActivityContextFactory;
import dev.webfx.stack.framework.client.activity.impl.ActivityContextBase;

/**
 * @author Bruno Salmon
 */
public class ActivePropertyActivityContextBase
        <THIS extends ActivePropertyActivityContextBase<THIS>>

        extends ActivityContextBase<THIS>
        implements ActivePropertyActivityContext<THIS> {

    protected ActivePropertyActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    private final Property<Boolean> activeProperty = new SimpleObjectProperty<>(false);
    @Override
    public ReadOnlyProperty<Boolean> activeProperty() {
        return activeProperty;
    }

}
