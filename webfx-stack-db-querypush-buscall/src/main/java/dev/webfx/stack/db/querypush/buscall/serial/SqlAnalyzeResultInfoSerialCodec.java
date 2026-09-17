package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;

public final class SqlAnalyzeResultInfoSerialCodec extends SerialCodecBase<SqlAnalyzeResultInfo> {

    private static final String CODEC_ID = "SqlAnalyzeResultInfo";
    private static final String STATEMENT_KEY = "statement";
    private static final String STATUS_KEY = "status";
    private static final String PLAN_KEY = "plan";
    private static final String DQL_KEY = "dql";
    private static final String PARAMETERS_KEY = "parameters";
    private static final String CAPTURED_AGE_MILLIS_KEY = "capturedAgeMillis";
    private static final String CLIENT_VERSION_KEY = "clientVersion";

    public SqlAnalyzeResultInfoSerialCodec() {
        super(SqlAnalyzeResultInfo.class, CODEC_ID);
    }

    @Override
    public void encode(SqlAnalyzeResultInfo arg, AstObject serial) {
        encodeString(serial, STATEMENT_KEY,           arg.getStatement());
        encodeString(serial, STATUS_KEY,              arg.getStatus());
        encodeString(serial, PLAN_KEY,                arg.getPlan());
        encodeString(serial, DQL_KEY,                 arg.getDql());
        encodeString(serial, PARAMETERS_KEY,          arg.getParameters());
        encodeLong(  serial, CAPTURED_AGE_MILLIS_KEY, arg.getCapturedAgeMillis());
        encodeString(serial, CLIENT_VERSION_KEY,      arg.getClientVersion());
    }

    @Override
    public SqlAnalyzeResultInfo decode(ReadOnlyAstObject serial) {
        return new SqlAnalyzeResultInfo(
            decodeString(serial, STATEMENT_KEY),
            decodeString(serial, STATUS_KEY),
            decodeString(serial, PLAN_KEY),
            decodeString(serial, DQL_KEY),
            decodeString(serial, PARAMETERS_KEY),
            orMinusOne(decodeLong(serial, CAPTURED_AGE_MILLIS_KEY)),
            decodeString(serial, CLIENT_VERSION_KEY));
    }

    private static long orMinusOne(Long l) {
        return l == null ? -1L : l;
    }
}
