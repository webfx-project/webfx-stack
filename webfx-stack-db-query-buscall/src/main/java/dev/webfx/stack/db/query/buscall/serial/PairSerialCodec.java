package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * This module doesn't need to serialise Pair directly, but the cache entry used in ReactiveCall indirectly needs it (as
 * the cached value is of type Pair<QueryArgument, QueryResult>). So we provide here the serial codec for Pair.
 *
 * @author Bruno Salmon
 */
public class PairSerialCodec extends SerialCodecBase<Pair> {

    private static final String CODEC_ID = "Pair";
    private static final String ONE = "1";
    private static final String TWO = "2";

    public PairSerialCodec() {
        super(Pair.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(Pair arg, AstObject json) {
        json.set(ONE, SerialCodecManager.encodeToJson(arg.get1()));
        json.set(TWO, SerialCodecManager.encodeToJson(arg.get2()));
    }

    @Override
    public Pair decodeFromJson(ReadOnlyAstObject json) {
        return new Pair(
                SerialCodecManager.decodeFromJson(json.getObject(ONE)),
                SerialCodecManager.decodeFromJson(json.getObject(TWO))
        );
    }
}
