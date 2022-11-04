package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.query.QueryArgument;

public final class QueryArgumentSerialCodec extends SerialCodecBase<QueryArgument> {

    private static final String CODEC_ID = "QueryArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String DATA_SCOPE_KEY = "dataScope";
    private static final String LANGUAGE_KEY = "lang";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";

    public QueryArgumentSerialCodec() {
        super(QueryArgument.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(QueryArgument arg, WritableJsonObject json) {
        json.set(DATA_SOURCE_ID_KEY, arg.getDataSourceId());
        json.set(DATA_SCOPE_KEY, SerialCodecManager.encodeToJson(arg.getDataScope()));
        json.set(LANGUAGE_KEY, arg.getLanguage());
        json.set(STATEMENT_KEY, arg.getStatement());
        if (!Arrays.isEmpty(arg.getParameters()))
            json.set(PARAMETERS_KEY, SerialCodecManager.encodePrimitiveArrayToJsonArray(arg.getParameters()));
    }

    @Override
    public QueryArgument decodeFromJson(JsonObject json) {
        return new QueryArgument(null,
                json.get(DATA_SOURCE_ID_KEY),
                SerialCodecManager.decodeFromJson(json.getObject(DATA_SCOPE_KEY)),
                json.getString(LANGUAGE_KEY),
                json.getString(STATEMENT_KEY),
                SerialCodecManager.decodePrimitiveArrayFromJsonArray(json.getArray(PARAMETERS_KEY))
        );
    }
}
