package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
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
    public void encode(QueryResultTranslation arg, AstObject serial) {
        encodeObject( serial, ROWS_BEFORE_KEY,      arg.getRowsBefore());
        encodeInteger(serial, ROW_START_KEY,        arg.getRowStart());
        encodeInteger(serial, ROW_END_KEY,          arg.getRowEnd());
        encodeObject( serial, ROWS_AFTER_KEY,       arg.getRowsAfter());
        encodeInteger(serial, PREVIOUS_VERSION_KEY, arg.getPreviousQueryResultVersionNumber());
        encodeInteger(serial, FINAL_VERSION_KEY,    arg.getFinalQueryResultVersionNumber());
    }

    @Override
    public QueryResultTranslation decode(ReadOnlyAstObject serial) {
        return new QueryResultTranslation(
                decodeObject( serial, ROWS_BEFORE_KEY),
                decodeInteger(serial, ROW_START_KEY),
                decodeInteger(serial, ROW_END_KEY),
                decodeObject( serial, ROWS_AFTER_KEY),
                decodeInteger(serial, PREVIOUS_VERSION_KEY),
                decodeInteger(serial, FINAL_VERSION_KEY)
        );
    }
}
