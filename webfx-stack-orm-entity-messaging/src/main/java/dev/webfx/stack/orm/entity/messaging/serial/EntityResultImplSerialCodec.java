package dev.webfx.stack.orm.entity.messaging.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.result.impl.EntityResultImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class EntityResultImplSerialCodec extends SerialCodecBase<EntityResultImpl> {

    private static final String CODEC_ID = "EntityResultImpl";

    private static final String ENTITIES_KEY = "entities";
    private static final String CLASS_KEY = "class";
    private static final String PRIMARY_KEY = "pk";
    private static final String VALUES_KEY = "val";

    public EntityResultImplSerialCodec() {
        super(EntityResultImpl.class, CODEC_ID);
    }

    @Override
    public void encode(EntityResultImpl o, AstObject serial) {
        AstArray entities = AST.createArray();
        for (EntityId entityId : o.getEntityIds()) {
            AstObject entity = AST.createObject();
            entity.set(CLASS_KEY, entityId.getDomainClass().getId());
            entity.set(PRIMARY_KEY, entityId.getPrimaryKey());
            AstObject values = AST.createObject();
            for (Object fieldId : o.getFieldIds(entityId)) {
                Object fieldValue = o.getFieldValue(entityId, fieldId);
                if (fieldValue instanceof EntityId)
                    fieldValue = SerialCodecManager.encodeToJson(fieldValue);
                values.set(fieldId.toString(), fieldValue);
            }
            entity.set(VALUES_KEY, values);
            entities.push(entity);
        }
        serial.setArray(ENTITIES_KEY, entities);
    }

    @Override
    public EntityResultImpl decode(ReadOnlyAstObject serial) {
        List<EntityId> entityIds = new ArrayList<>();
        List<Map> entityFieldsMaps = new ArrayList<>();
        ReadOnlyAstArray entities = serial.getArray(ENTITIES_KEY);
        for (int i = 0; i < entities.size(); i++) {
            ReadOnlyAstObject entity = entities.getObject(i);
            Object classId = entity.get(CLASS_KEY);
            Object primaryKey = entity.get(PRIMARY_KEY);
            EntityId entityId = EntityId.create(classId, primaryKey);
            Map fieldsMap = new HashMap();
            ReadOnlyAstObject values = entity.getObject(VALUES_KEY);
            for (Object key : values.keys()) {
                Object value = values.get(key.toString());
                if (AST.isObject(value))
                    value = SerialCodecManager.decodeFromJson(value);
                fieldsMap.put(key, value);
            }
            entityIds.add(entityId);
            entityFieldsMaps.add(fieldsMap);
        }
        return new EntityResultImpl(entityIds, entityFieldsMaps);
    }
}
