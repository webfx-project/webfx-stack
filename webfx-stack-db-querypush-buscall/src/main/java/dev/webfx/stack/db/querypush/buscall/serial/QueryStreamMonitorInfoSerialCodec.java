package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.QueryStreamMonitorInfo;

public final class QueryStreamMonitorInfoSerialCodec extends SerialCodecBase<QueryStreamMonitorInfo> {

    private static final String CODEC_ID = "QueryStreamMonitorInfo";
    private static final String QUERY_STREAM_IDS_KEY = "queryStreamIds";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";
    private static final String ROW_COUNT_KEY = "rowCount";
    private static final String STREAM_COUNT_KEY = "streamCount";
    private static final String ACTIVE_STREAM_COUNT_KEY = "activeStreamCount";
    private static final String CLIENTS_COUNT_KEY = "clientsCount";
    private static final String USERS_COUNT_KEY = "usersCount";
    private static final String LAST_EXECUTION_AGE_MILLIS_KEY = "lastExecutionAgeMillis";
    private static final String ORIGIN_KEY = "origin";

    public QueryStreamMonitorInfoSerialCodec() {
        super(QueryStreamMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(QueryStreamMonitorInfo arg, AstObject serial) {
        encodeObjectArray(serial, QUERY_STREAM_IDS_KEY,         arg.getQueryStreamIds());
        encodeString(     serial, STATEMENT_KEY,                arg.getStatement());
        encodeString(     serial, PARAMETERS_KEY,               arg.getParameters());
        encodeInteger(    serial, ROW_COUNT_KEY,                arg.getRowCount());
        encodeInteger(    serial, STREAM_COUNT_KEY,             arg.getStreamCount());
        encodeInteger(    serial, ACTIVE_STREAM_COUNT_KEY,      arg.getActiveStreamCount());
        encodeInteger(    serial, CLIENTS_COUNT_KEY,            arg.getClientsCount());
        encodeInteger(    serial, USERS_COUNT_KEY,              arg.getUsersCount());
        encodeLong(       serial, LAST_EXECUTION_AGE_MILLIS_KEY, arg.getLastExecutionAgeMillis());
        encodeString(     serial, ORIGIN_KEY,                   arg.getOrigin());
    }

    @Override
    public QueryStreamMonitorInfo decode(ReadOnlyAstObject serial) {
        Long lastExecutionAgeMillis = decodeLong(serial, LAST_EXECUTION_AGE_MILLIS_KEY);
        return new QueryStreamMonitorInfo(
                decodeObjectArray(serial, QUERY_STREAM_IDS_KEY),
                decodeString(     serial, STATEMENT_KEY),
                decodeString(     serial, PARAMETERS_KEY),
                decodeInteger(    serial, ROW_COUNT_KEY, -1),
                decodeInteger(    serial, STREAM_COUNT_KEY, 0),
                decodeInteger(    serial, ACTIVE_STREAM_COUNT_KEY, 0),
                decodeInteger(    serial, CLIENTS_COUNT_KEY, 0),
                decodeInteger(    serial, USERS_COUNT_KEY, 0),
                lastExecutionAgeMillis == null ? -1 : lastExecutionAgeMillis,
                decodeString(     serial, ORIGIN_KEY)
        );
    }

}
