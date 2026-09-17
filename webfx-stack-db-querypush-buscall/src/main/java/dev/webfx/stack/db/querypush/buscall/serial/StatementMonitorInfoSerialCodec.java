package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.StatementMonitorInfo;

public final class StatementMonitorInfoSerialCodec extends SerialCodecBase<StatementMonitorInfo> {

    private static final String CODEC_ID = "StatementMonitorInfo";
    private static final String STATEMENT_KEY = "statement";
    private static final String KIND_KEY = "kind";
    private static final String COUNT_KEY = "count";
    private static final String TOTAL_NANOS_KEY = "totalNanos";
    private static final String MAX_NANOS_KEY = "maxNanos";
    private static final String ORIGIN_KEY = "origin";

    public StatementMonitorInfoSerialCodec() {
        super(StatementMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(StatementMonitorInfo arg, AstObject serial) {
        encodeString(serial, STATEMENT_KEY,   arg.getStatement());
        encodeString(serial, KIND_KEY,        arg.getKind());
        encodeLong(  serial, COUNT_KEY,       arg.getCount());
        encodeLong(  serial, TOTAL_NANOS_KEY, arg.getTotalNanos());
        encodeLong(  serial, MAX_NANOS_KEY,   arg.getMaxNanos());
        encodeString(serial, ORIGIN_KEY,      arg.getOrigin());
    }

    @Override
    public StatementMonitorInfo decode(ReadOnlyAstObject serial) {
        return new StatementMonitorInfo(
            decodeString(serial, STATEMENT_KEY),
            decodeString(serial, KIND_KEY),
            orZero(decodeLong(serial, COUNT_KEY)),
            orZero(decodeLong(serial, TOTAL_NANOS_KEY)),
            orZero(decodeLong(serial, MAX_NANOS_KEY)),
            decodeString(serial, ORIGIN_KEY));
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
