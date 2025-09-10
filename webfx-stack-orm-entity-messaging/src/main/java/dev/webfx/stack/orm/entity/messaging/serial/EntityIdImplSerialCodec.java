package dev.webfx.stack.orm.entity.messaging.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.orm.entity.impl.EntityIdImpl;

/**
 * @author Bruno Salmon
 */
public final class EntityIdImplSerialCodec extends SerialCodecBase<EntityIdImpl> {

    private static final String CODEC_ID = "EntityIdImpl";

    private static final String CLASS_KEY = "class";
    private static final String PRIMARY_KEY = "pk";

    public EntityIdImplSerialCodec() {
        super(EntityIdImpl.class, CODEC_ID);
    }

    @Override
    public void encode(EntityIdImpl o, AstObject serial) {
        serial.set(CLASS_KEY,   o.getDomainClass().getId());
        serial.set(PRIMARY_KEY, o.getPrimaryKey());
    }

    @Override
    public EntityIdImpl decode(ReadOnlyAstObject serial) {
        return EntityIdImpl.create(serial.getObject(CLASS_KEY), serial.getObject(PRIMARY_KEY));
    }
}
