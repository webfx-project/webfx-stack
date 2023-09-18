package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.diff.impl.QueryResultTranslation;

public final class QueryResultTranslationSerialCodec extends SerialCodecBase<QueryResultTranslation> {

    private static final String CODEC_ID = "QueryResultTranslation";
    private static final String ROWS_BEFORE_KEY = "rowsBefore";
    private static final String ROW_START_KEY = "rowStart";
    private static final String ROW_END_KEY = "rowEnd";
    private static final String ROWS_AFTER_KEY = "rowsAfter";
    private static final String PREVIOUS_VERSION_KEY = "previousVersion";
    private static final String FINAL_VERSION_KEY = "finalVersion";

    public QueryResultTranslationSerialCodec() {
        super(QueryResultTranslation.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(QueryResultTranslation arg, AstObject json) {
        SerialCodecBase.encodeKeyIfNotNull(ROWS_BEFORE_KEY, arg.getRowsBefore(), json);
        SerialCodecBase.encodeKey(ROW_START_KEY, arg.getRowStart(), json);
        SerialCodecBase.encodeKey(ROW_END_KEY, arg.getRowEnd(), json);
        SerialCodecBase.encodeKeyIfNotNull(ROWS_AFTER_KEY, arg.getRowsAfter(), json);
        SerialCodecBase.encodeKey(PREVIOUS_VERSION_KEY, arg.getPreviousQueryResultVersionNumber(), json);
        SerialCodecBase.encodeKey(FINAL_VERSION_KEY, arg.getFinalQueryResultVersionNumber(), json);
    }

    @Override
    public QueryResultTranslation decodeFromJson(ReadOnlyAstObject json) {
        return new QueryResultTranslation(
                SerialCodecManager.decodeFromJson(json.get(ROWS_BEFORE_KEY)),
                json.getInteger(ROW_START_KEY),
                json.getInteger(ROW_END_KEY),
                SerialCodecManager.decodeFromJson(json.get(ROWS_AFTER_KEY)),
                json.getInteger(PREVIOUS_VERSION_KEY),
                json.getInteger(FINAL_VERSION_KEY)
        );
    }
}
