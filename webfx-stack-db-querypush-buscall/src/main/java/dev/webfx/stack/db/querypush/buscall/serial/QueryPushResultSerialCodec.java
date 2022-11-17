package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.QueryPushResult;

public final class QueryPushResultSerialCodec extends SerialCodecBase<QueryPushResult> {

    private static final String CODEC_ID = "QueryPushResult";
    private static final String QUERY_STREAM_ID_KEY = "queryStreamId";
    private static final String QUERY_RESULT_KEY = "queryResult";
    private static final String QUERY_RESULT_DIFF_KEY = "queryResultDiff";

    public QueryPushResultSerialCodec() {
        super(QueryPushResult.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(QueryPushResult arg, WritableJsonObject json) {
        SerialCodecBase.encodeKeyIfNotNull(QUERY_STREAM_ID_KEY, arg.getQueryStreamId(), json);
        SerialCodecBase.encodeKeyIfNotNull(QUERY_RESULT_KEY, arg.getQueryResult(), json);
        SerialCodecBase.encodeKeyIfNotNull(QUERY_RESULT_DIFF_KEY, arg.getQueryResultDiff(), json);
    }

    @Override
    public QueryPushResult decodeFromJson(JsonObject json) {
        return new QueryPushResult(
                json.get(QUERY_STREAM_ID_KEY),
                SerialCodecManager.decodeFromJson(json.get(QUERY_RESULT_KEY)),
                SerialCodecManager.decodeFromJson(json.get(QUERY_RESULT_DIFF_KEY))
        );
    }
}
