package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.ActiveDbQueryInfo;
import dev.webfx.stack.db.querypush.DatabaseHealthMonitorInfo;

public final class DatabaseHealthMonitorInfoSerialCodec extends SerialCodecBase<DatabaseHealthMonitorInfo> {

    private static final String CODEC_ID = "DatabaseHealthMonitorInfo";
    private static final String MAX_CONNECTIONS_KEY = "maxConnections";
    private static final String TOTAL_CONNECTIONS_KEY = "totalConnections";
    private static final String ACTIVE_CONNECTIONS_KEY = "activeConnections";
    private static final String IDLE_CONNECTIONS_KEY = "idleConnections";
    private static final String NOT_INSPECTABLE_CONNECTIONS_KEY = "notInspectableConnections";
    private static final String SELF_CONNECTIONS_KEY = "selfConnections";
    private static final String ACTIVE_QUERIES_KEY = "activeQueries";

    public DatabaseHealthMonitorInfoSerialCodec() {
        super(DatabaseHealthMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(DatabaseHealthMonitorInfo arg, AstObject serial) {
        encodeInteger(serial, MAX_CONNECTIONS_KEY,    arg.getMaxConnections());
        encodeInteger(serial, TOTAL_CONNECTIONS_KEY,  arg.getTotalConnections());
        encodeInteger(serial, ACTIVE_CONNECTIONS_KEY, arg.getActiveConnections());
        encodeInteger(serial, IDLE_CONNECTIONS_KEY,   arg.getIdleConnections());
        encodeInteger(serial, NOT_INSPECTABLE_CONNECTIONS_KEY, arg.getNotInspectableConnections());
        encodeInteger(serial, SELF_CONNECTIONS_KEY,   arg.getSelfConnections());
        encodeArray(  serial, ACTIVE_QUERIES_KEY,     arg.getActiveQueries());
    }

    @Override
    public DatabaseHealthMonitorInfo decode(ReadOnlyAstObject serial) {
        return new DatabaseHealthMonitorInfo(
            decodeInteger(serial, MAX_CONNECTIONS_KEY, -1),
            decodeInteger(serial, TOTAL_CONNECTIONS_KEY, 0),
            decodeInteger(serial, ACTIVE_CONNECTIONS_KEY, 0),
            decodeInteger(serial, IDLE_CONNECTIONS_KEY, 0),
            // Defaults to 0, so a client on this build still decodes a snapshot from an
            // older server (which sends no such key) instead of failing.
            decodeInteger(serial, NOT_INSPECTABLE_CONNECTIONS_KEY, 0),
            // Same reason: an older server sends no self count, and 0 reads as "none reported"
            // rather than breaking the decode.
            decodeInteger(serial, SELF_CONNECTIONS_KEY, 0),
            decodeArray(  serial, ACTIVE_QUERIES_KEY, ActiveDbQueryInfo.class));
    }
}
