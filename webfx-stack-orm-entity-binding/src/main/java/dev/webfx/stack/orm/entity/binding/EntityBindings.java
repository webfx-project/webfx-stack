package dev.webfx.stack.orm.entity.binding;

import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.impl.DynamicEntity;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;

import java.util.function.Supplier;

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
            changesBuilder.setHasChangesPropertyUpdater(hasChangesProperty::set);
        }
        return hasChangesProperty;
    }

    public static BooleanProperty getBooleanFieldProperty(Entity entity, String fieldId) {
        return (BooleanProperty) getFieldProperty(entity, fieldId, SimpleBooleanProperty::new);
    }

    public static StringProperty getStringFieldProperty(Entity entity, String fieldId) {
        return (StringProperty) getFieldProperty(entity, fieldId, SimpleStringProperty::new);
    }

    public static IntegerProperty getIntegerFieldProperty(Entity entity, String fieldId) {
        return (IntegerProperty) getFieldProperty(entity, fieldId, SimpleIntegerProperty::new);
    }

    public static DoubleProperty getDoubleFieldProperty(Entity entity, String fieldId) {
        return (DoubleProperty) getFieldProperty(entity, fieldId, SimpleDoubleProperty::new);
    }

    private static Property getFieldProperty(Entity entity, String fieldId, Supplier<Property> propertyFactory) {
        // Checking if that field property has already been instantiated
        DynamicEntity dynamicEntity = (DynamicEntity) entity;
        Property fieldProperty = (Property) dynamicEntity.getFieldProperty(fieldId);
        if (fieldProperty == null) { // if not, we create it and initialize it
            Property finalFieldProperty = fieldProperty = propertyFactory.get();
            // Setting its initial value
            fieldProperty.setValue(entity.getFieldValue(fieldId));
            // Changes made on this property will be applied back to the entity
            fieldProperty.addListener(observable -> entity.setFieldValue(fieldId, finalFieldProperty.getValue()));
            // And changes to the entity will be sent back to the property
            DynamicEntity.setFieldPropertyUpdater(EntityBindings::onEntityFieldValueChanged);
            // Memorizing this new field property into the entity
            dynamicEntity.setFieldProperty(fieldId, fieldProperty);
        }
        return fieldProperty;
    }

    private static void onEntityFieldValueChanged(Object fieldProperty, Object value) {
        ((Property) fieldProperty).setValue(value);
    }

}
