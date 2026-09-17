package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.ActiveDbQueryInfo;

public final class ActiveDbQueryInfoSerialCodec extends SerialCodecBase<ActiveDbQueryInfo> {

    private static final String CODEC_ID = "ActiveDbQueryInfo";
    // Wire keys MUST match the client (@kbs3/shared) field names — the React client reads them by name.
    private static final String PID_KEY = "pid";
    private static final String DURATION_MILLIS_KEY = "durationMillis";
    private static final String STATE_KEY = "state";
    private static final String WAIT_EVENT_TYPE_KEY = "waitEventType";
    private static final String BLOCKED_BY_PID_KEY = "blockedByPid";
    private static final String QUERY_KEY = "query";

    public ActiveDbQueryInfoSerialCodec() {
        super(ActiveDbQueryInfo.class, CODEC_ID);
    }

    @Override
    public void encode(ActiveDbQueryInfo arg, AstObject serial) {
        encodeInteger(serial, PID_KEY,             arg.getPid());
        encodeLong(   serial, DURATION_MILLIS_KEY, arg.getDurationMillis());
        encodeString( serial, STATE_KEY,           arg.getState());
        encodeString( serial, WAIT_EVENT_TYPE_KEY, arg.getWaitEventType());
        encodeInteger(serial, BLOCKED_BY_PID_KEY,  arg.getBlockedByPid());
        encodeString( serial, QUERY_KEY,           arg.getQuery());
    }

    @Override
    public ActiveDbQueryInfo decode(ReadOnlyAstObject serial) {
        return new ActiveDbQueryInfo(
            orZero(decodeInteger(serial, PID_KEY)),
            orZeroL(decodeLong(serial, DURATION_MILLIS_KEY)),
            decodeString(serial, STATE_KEY),
            decodeString(serial, WAIT_EVENT_TYPE_KEY),
            orZero(decodeInteger(serial, BLOCKED_BY_PID_KEY)),
            decodeString(serial, QUERY_KEY));
    }

    private static int orZero(Integer i) {
        return i == null ? 0 : i;
    }

    private static long orZeroL(Long l) {
        return l == null ? 0L : l;
    }
}
