package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
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
    public void encode(QueryPushResult arg, AstObject serial) {
        encodeObject(serial, QUERY_STREAM_ID_KEY, arg.getQueryStreamId());
        encodeObject(serial, QUERY_RESULT_KEY, arg.getQueryResult());
        encodeObject(serial, QUERY_RESULT_DIFF_KEY, arg.getQueryResultDiff());
    }

    @Override
    public QueryPushResult decode(ReadOnlyAstObject serial) {
        return new QueryPushResult(
                decodeObject(serial, QUERY_STREAM_ID_KEY),
                decodeObject(serial, QUERY_RESULT_KEY),
                decodeObject(serial, QUERY_RESULT_DIFF_KEY)
        );
    }
}
