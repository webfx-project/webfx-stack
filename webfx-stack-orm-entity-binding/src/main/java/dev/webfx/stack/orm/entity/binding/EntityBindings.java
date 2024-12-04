package dev.webfx.stack.orm.entity.binding;

import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Bruno Salmon
 */
public final class EntityBindings {

    public static BooleanExpression hasChangesProperty(UpdateStore updateStore) {
        UpdateStoreImpl updateStoreImpl = (UpdateStoreImpl) updateStore;
        BooleanProperty hasChangesProperty = (BooleanProperty) updateStoreImpl.getHasChangesProperty();
        if (hasChangesProperty == null) {
            EntityChangesBuilder changesBuilder = updateStoreImpl.getChangesBuilder();
            hasChangesProperty = new SimpleBooleanProperty(changesBuilder.hasChanges());
            changesBuilder.setHasChangesHandler(hasChangesProperty::set);
        }
        return hasChangesProperty;
    }

}
