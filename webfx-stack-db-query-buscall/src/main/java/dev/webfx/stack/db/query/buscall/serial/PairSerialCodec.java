package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.tuples.Pair;
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
    public void encode(Pair arg, AstObject serial) {
        encodeObject(serial, ONE, arg.get1());
        encodeObject(serial, TWO, arg.get2());
    }

    @Override
    public Pair decode(ReadOnlyAstObject serial) {
        return new Pair(
                decodeObject(serial, ONE),
                decodeObject(serial, TWO)
        );
    }
}
