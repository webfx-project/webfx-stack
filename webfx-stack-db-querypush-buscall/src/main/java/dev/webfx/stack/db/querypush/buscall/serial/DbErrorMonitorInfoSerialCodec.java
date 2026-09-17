package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.DbErrorMonitorInfo;

public final class DbErrorMonitorInfoSerialCodec extends SerialCodecBase<DbErrorMonitorInfo> {

    private static final String CODEC_ID = "DbErrorMonitorInfo";
    private static final String EPOCH_MILLIS_KEY = "epochMillis";
    private static final String KIND_KEY = "kind";
    private static final String STATEMENT_KEY = "statement";
    private static final String MESSAGE_KEY = "message";
    private static final String ORIGIN_KEY = "origin";

    public DbErrorMonitorInfoSerialCodec() {
        super(DbErrorMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(DbErrorMonitorInfo arg, AstObject serial) {
        encodeLong(  serial, EPOCH_MILLIS_KEY, arg.getEpochMillis());
        encodeString(serial, KIND_KEY,         arg.getKind());
        encodeString(serial, STATEMENT_KEY,    arg.getStatement());
        encodeString(serial, MESSAGE_KEY,      arg.getMessage());
        encodeString(serial, ORIGIN_KEY,       arg.getOrigin());
    }

    @Override
    public DbErrorMonitorInfo decode(ReadOnlyAstObject serial) {
        return new DbErrorMonitorInfo(
            orZero(decodeLong(serial, EPOCH_MILLIS_KEY)),
            decodeString(serial, KIND_KEY),
            decodeString(serial, STATEMENT_KEY),
            decodeString(serial, MESSAGE_KEY),
            decodeString(serial, ORIGIN_KEY));
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
