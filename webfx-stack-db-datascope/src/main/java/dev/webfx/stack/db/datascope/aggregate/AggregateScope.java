package dev.webfx.stack.db.datascope.aggregate;

import dev.webfx.stack.db.datascope.KeyDataScope;
import dev.webfx.stack.db.datascope.ScopeUtil;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class AggregateScope implements KeyDataScope {

    public static String KEY = "aggregate";

    private final Map<Object /* Aggregate type */, Object[] /* aggregate keys */> aggregates;

    public AggregateScope(Map<Object, Object[]> aggregates) {
        this.aggregates = aggregates;
    }

    @Override
    public Object getKey() {
        return KEY;
    }

    @Override
    public boolean intersects(KeyDataScope otherScope) {
        return otherScope instanceof AggregateScope && intersects((AggregateScope) otherScope);
    }

    public boolean intersects(AggregateScope otherScope) {
        if (true) // Temporary disabled while the implementation doesn't work in all situations (ex: drag&drop in rooms graphic => only the new room is refresh, not the old room)
            return true; // TODO fix the implementation to make it work in all situations
        for (Map.Entry<Object, Object[]> entry : aggregates.entrySet()) {
            Object aggregateType = entry.getKey();
            Object[] otherAggregateKeys = otherScope.aggregates.get(aggregateType);
            if (otherAggregateKeys != null) {
                Object[] aggregateKeys = entry.getValue();
                if (ScopeUtil.arraysIntersect(aggregateKeys, otherAggregateKeys))
                    return true;
            }
        }
        return false;
    }

    public static AggregateScopeBuilder builder() {
        return new AggregateScopeBuilder();
    }

    /**************************************
     *           Serial Codec             *
     * ***********************************/

    public static final class ProvidedSerialCodec extends SerialCodecBase<AggregateScope> {

        private static final String CODEC_ID = "AggregateScope";

        public ProvidedSerialCodec() {
            super(AggregateScope.class, CODEC_ID);
        }

        @Override
        public void encodeToJson(AggregateScope arg, AstObject json) {
            for (Map.Entry<Object, Object[]> entry : arg.aggregates.entrySet())
                json.set(entry.getKey().toString(), SerialCodecManager.encodePrimitiveArrayToAstArray(entry.getValue()));
        }

        @Override
        public AggregateScope decodeFromJson(ReadOnlyAstObject json) {
            AggregateScopeBuilder asb = AggregateScope.builder();
            ReadOnlyAstArray keys = json.keys();
            for (int i = 1; i < keys.size(); i++) { // Skipping index 0 = $codec key (quite ugly)
                String key = keys.getString(i);
                ReadOnlyAstArray array = json.getArray(key);
                for (int j = 0; j < array.size(); j++)
                    asb.addAggregate(key, array.getElement(j));
            }
            return asb.build();
        }
    }
}
