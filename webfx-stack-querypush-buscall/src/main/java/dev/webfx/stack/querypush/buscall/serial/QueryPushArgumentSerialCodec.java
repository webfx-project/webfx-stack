package dev.webfx.stack.querypush.buscall.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.querypush.QueryPushArgument;

public final class QueryPushArgumentSerialCodec extends SerialCodecBase<QueryPushArgument> {

    private static final String CODEC_ID = "QueryPushArgument";
    private static final String QUERY_STREAM_ID_KEY = "queryStreamId";
    private static final String PARENT_QUERY_STREAM_ID_KEY = "parentQueryStreamId";
    private static final String CLIENT_PUSH_ID_KEY = "pushClientId";
    private static final String QUERY_ARGUMENT_KEY = "queryArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String ACTIVE_KEY = "active";
    private static final String RESEND_KEY = "resend";
    private static final String CLOSE_KEY = "close";

    public QueryPushArgumentSerialCodec() {
        super(QueryPushArgument.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(QueryPushArgument arg, WritableJsonObject json) {
        SerialCodecBase.encodeKeyIfNotNull(QUERY_STREAM_ID_KEY, arg.getQueryStreamId(), json);
        SerialCodecBase.encodeKeyIfNotNull(PARENT_QUERY_STREAM_ID_KEY, arg.getParentQueryStreamId(), json);
        SerialCodecBase.encodeKeyIfNotNull(CLIENT_PUSH_ID_KEY, arg.getPushClientId(), json);
        SerialCodecBase.encodeKeyIfNotNull(QUERY_ARGUMENT_KEY, arg.getQueryArgument(), json);
        SerialCodecBase.encodeKey(DATA_SOURCE_ID_KEY, arg.getDataSourceId(), json);
        SerialCodecBase.encodeKeyIfNotNull(ACTIVE_KEY, arg.getActive(), json);
        SerialCodecBase.encodeKeyIfNotNull(RESEND_KEY, arg.getResend(), json);
        SerialCodecBase.encodeKeyIfNotNull(CLOSE_KEY, arg.getClose(), json);
    }

    @Override
    public QueryPushArgument decodeFromJson(JsonObject json) {
        return new QueryPushArgument(
                json.get(QUERY_STREAM_ID_KEY),
                json.get(PARENT_QUERY_STREAM_ID_KEY),
                json.get(CLIENT_PUSH_ID_KEY),
                SerialCodecManager.decodeFromJson(json.get(QUERY_ARGUMENT_KEY)),
                json.get(DATA_SOURCE_ID_KEY),
                json.getBoolean(ACTIVE_KEY),
                json.getBoolean(RESEND_KEY),
                json.getBoolean(CLOSE_KEY),
                null
        );
    }

}
