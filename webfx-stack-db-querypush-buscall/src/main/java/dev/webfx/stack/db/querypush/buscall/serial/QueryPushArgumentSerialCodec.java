package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.QueryPushArgument;

public final class QueryPushArgumentSerialCodec extends SerialCodecBase<QueryPushArgument> {

    private static final String CODEC_ID = "QueryPushArgument";
    private static final String QUERY_STREAM_ID_KEY = "queryStreamId";
    private static final String PARENT_QUERY_STREAM_ID_KEY = "parentQueryStreamId";
    private static final String QUERY_ARGUMENT_KEY = "queryArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String ACTIVE_KEY = "active";
    private static final String RESEND_KEY = "resend";
    private static final String CLOSE_KEY = "close";

    public QueryPushArgumentSerialCodec() {
        super(QueryPushArgument.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(QueryPushArgument arg, AstObject json) {
        encodeKeyIfNotNull(QUERY_STREAM_ID_KEY, arg.getQueryStreamId(), json);
        encodeKeyIfNotNull(PARENT_QUERY_STREAM_ID_KEY, arg.getParentQueryStreamId(), json);
        encodeKeyIfNotNull(QUERY_ARGUMENT_KEY, arg.getQueryArgument(), json);
        encodeKey(DATA_SOURCE_ID_KEY, arg.getDataSourceId(), json);
        encodeKeyIfNotNull(ACTIVE_KEY, arg.getActive(), json);
        encodeKeyIfNotNull(RESEND_KEY, arg.getResend(), json);
        encodeKeyIfNotNull(CLOSE_KEY, arg.getClose(), json);
    }

    @Override
    public QueryPushArgument decodeFromJson(ReadOnlyAstObject json) {
        return new QueryPushArgument(
                json.get(QUERY_STREAM_ID_KEY),
                json.get(PARENT_QUERY_STREAM_ID_KEY),
                SerialCodecManager.decodeFromJson(json.get(QUERY_ARGUMENT_KEY)),
                json.get(DATA_SOURCE_ID_KEY),
                json.getBoolean(ACTIVE_KEY),
                json.getBoolean(RESEND_KEY),
                json.getBoolean(CLOSE_KEY),
                null
        );
    }

}
