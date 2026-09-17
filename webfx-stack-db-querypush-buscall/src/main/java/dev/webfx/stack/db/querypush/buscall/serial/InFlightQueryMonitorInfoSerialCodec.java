package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.InFlightQueryMonitorInfo;

public final class InFlightQueryMonitorInfoSerialCodec extends SerialCodecBase<InFlightQueryMonitorInfo> {

    private static final String CODEC_ID = "InFlightQueryMonitorInfo";
    private static final String ID_KEY = "id";
    private static final String KIND_KEY = "kind";
    private static final String STATEMENT_KEY = "statement";
    private static final String AGE_MILLIS_KEY = "ageMillis";
    private static final String ORIGIN_KEY = "origin";

    public InFlightQueryMonitorInfoSerialCodec() {
        super(InFlightQueryMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(InFlightQueryMonitorInfo arg, AstObject serial) {
        encodeLong(  serial, ID_KEY,         arg.getId());
        encodeString(serial, KIND_KEY,       arg.getKind());
        encodeString(serial, STATEMENT_KEY,  arg.getStatement());
        encodeLong(  serial, AGE_MILLIS_KEY, arg.getAgeMillis());
        encodeString(serial, ORIGIN_KEY,     arg.getOrigin());
    }

    @Override
    public InFlightQueryMonitorInfo decode(ReadOnlyAstObject serial) {
        return new InFlightQueryMonitorInfo(
            orZero(decodeLong(serial, ID_KEY)),
            decodeString(serial, KIND_KEY),
            decodeString(serial, STATEMENT_KEY),
            orZero(decodeLong(serial, AGE_MILLIS_KEY)),
            decodeString(serial, ORIGIN_KEY));
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
