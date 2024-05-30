package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
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
    public void encode(QueryPushArgument arg, AstObject serial) {
        encodeObject( serial, QUERY_STREAM_ID_KEY,        arg.getQueryStreamId());
        encodeObject( serial, PARENT_QUERY_STREAM_ID_KEY, arg.getParentQueryStreamId());
        encodeObject( serial, QUERY_ARGUMENT_KEY,         arg.getQueryArgument());
        encodeObject( serial, DATA_SOURCE_ID_KEY,         arg.getDataSourceId());
        encodeBoolean(serial, ACTIVE_KEY,                 arg.getActive());
        encodeBoolean(serial, RESEND_KEY,                 arg.getResend());
        encodeBoolean(serial, CLOSE_KEY,                  arg.getClose());
    }

    @Override
    public QueryPushArgument decode(ReadOnlyAstObject serial) {
        return new QueryPushArgument(
                decodeObject( serial, QUERY_STREAM_ID_KEY),
                decodeObject( serial, PARENT_QUERY_STREAM_ID_KEY),
                decodeObject( serial, QUERY_ARGUMENT_KEY),
                decodeObject( serial, DATA_SOURCE_ID_KEY),
                decodeBoolean(serial, ACTIVE_KEY),
                decodeBoolean(serial, RESEND_KEY),
                decodeBoolean(serial, CLOSE_KEY),
                null
        );
    }

}
