package dev.webfx.stack.orm.entity.messaging.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.orm.entity.EntityStoreQuery;

/**
 * EntityStoreQuery is not sent to the server, so it doesn't require serialization for the network, but it is cached
 * locally for the client side, when using EntityStore.executeCachedQueryBatch(), and this is this caching that requires
 * serialization.
 *
 * @author Bruno Salmon
 */
public final class EntityStoreQuerySerialCodec extends SerialCodecBase<EntityStoreQuery> {

    private static final String CODEC_ID = "EntityStoreQuery";

    private static final String SELECT_KEY = "select";
    private static final String LIST_ID_KEY = "listId";
    private static final String PARAMETERS_KEY = "params";
    private static final String PARAMETER_NAMES_KEY = "names";

    public EntityStoreQuerySerialCodec() {
        super(EntityStoreQuery.class, CODEC_ID);
    }

    @Override
    public void encode(EntityStoreQuery o, AstObject serial) {
        encodeString(      serial, SELECT_KEY,          o.getSelect());
        encodeObject(      serial, LIST_ID_KEY,         o.getListId());
        encodeArray(       serial, PARAMETERS_KEY,      o.getParameters());
        encodeStringArray( serial, PARAMETER_NAMES_KEY, o.getParameterNames());
    }

    @Override
    public EntityStoreQuery decode(ReadOnlyAstObject serial) {
        return new EntityStoreQuery(
            decodeString(     serial, SELECT_KEY),
            decodeObject(     serial, LIST_ID_KEY),
            decodeArray(      serial, PARAMETERS_KEY),
            decodeStringArray(serial, PARAMETER_NAMES_KEY)
        );
    }
}
