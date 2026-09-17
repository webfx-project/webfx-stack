package dev.webfx.stack.db.datascope.aggregate;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.datascope.KeyDataScope;
import dev.webfx.stack.db.datascope.ScopeUtil;

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
        // Partition semantics: each aggregate TYPE is an independent dimension.
        // Two scopes are provably disjoint only when they share a type whose key
        // sets don't intersect — so common types are ANDed, and having no common
        // type at all means "cannot prove disjoint" and must answer true
        // (conservative: a missed dimension can only cost an extra refresh,
        // never a missed one). The previous (disabled) implementation answered
        // false when no type matched, which silently dropped refreshes — the
        // very reason it had been hack-disabled.
        for (Map.Entry<Object, Object[]> entry : aggregates.entrySet()) {
            Object[] otherAggregateKeys = otherScope.aggregates.get(entry.getKey());
            if (otherAggregateKeys != null && !ScopeUtil.arraysIntersect(entry.getValue(), otherAggregateKeys))
                return false; // common dimension with provably disjoint partitions
        }
        return true;
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
        public void encode(AggregateScope arg, AstObject serial) {
            for (Map.Entry<Object, Object[]> entry : arg.aggregates.entrySet())
                encodeArray(serial, entry.getKey().toString(), entry.getValue());
        }

        @Override
        public AggregateScope decode(ReadOnlyAstObject serial) {
            AggregateScopeBuilder asb = AggregateScope.builder();
            ReadOnlyAstArray keys = serial.keys();
            for (int i = 1; i < keys.size(); i++) { // Skipping index 0 = $codec key (quite ugly)
                String key = keys.getString(i);
                ReadOnlyAstArray array = serial.getArray(key);
                for (int j = 0; j < array.size(); j++)
                    asb.addAggregate(key, array.getElement(j));
            }
            return asb.build();
        }
    }
}
