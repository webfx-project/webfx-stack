package dev.webfx.stack.platform.shared.datascope.aggregate;

import dev.webfx.stack.platform.shared.datascope.KeyDataScope;
import dev.webfx.stack.platform.shared.datascope.ScopeUtil;
import dev.webfx.stack.platform.json.JsonArray;
import dev.webfx.stack.platform.json.JsonObject;
import dev.webfx.stack.platform.json.WritableJsonObject;
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
        public void encodeToJson(AggregateScope arg, WritableJsonObject json) {
            for (Map.Entry<Object, Object[]> entry : arg.aggregates.entrySet())
                json.set(entry.getKey().toString(), SerialCodecManager.encodePrimitiveArrayToJsonArray(entry.getValue()));
        }

        @Override
        public AggregateScope decodeFromJson(JsonObject json) {
            AggregateScopeBuilder asb = AggregateScope.builder();
            JsonArray keys = json.keys();
            for (int i = 1; i < keys.size(); i++) { // Skipping index 0 = $codec key (quite ugly)
                String key = keys.getString(i);
                JsonArray array = json.getArray(key);
                for (int j = 0; j < array.size(); j++)
                    asb.addAggregate(key, array.getNativeElement(j));
            }
            return asb.build();
        }
    }
}
