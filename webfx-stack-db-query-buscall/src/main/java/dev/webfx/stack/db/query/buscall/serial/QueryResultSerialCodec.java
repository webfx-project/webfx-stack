package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.buscall.serial.compression.repeat.RepeatedValuesCompressor;

public final class QueryResultSerialCodec extends SerialCodecBase<QueryResult> {

    public static boolean COMPRESSION = true; // Not final as this flag is turned off by the kbs2-model-import module to make the domain model snapshot
    private final static String CODEC_ID = "QueryResult";
    private final static String COLUMN_NAMES_KEY = "columnNames";
    private final static String COLUMN_COUNT_KEY = "columnCount";
    private final static String VALUES_KEY = "values";
    private final static String COMPRESSED_VALUES_KEY = "cvalues";
    private final static String VERSION_KEY = "version";

    public QueryResultSerialCodec() {
        super(QueryResult.class, CODEC_ID);
    }

    @Override
    public void encode(QueryResult rs, AstObject serial) {
        int columnCount = rs.getColumnCount();
        encodeStringArray(serial, COLUMN_NAMES_KEY, rs.getColumnNames());
        encodeInteger(serial, COLUMN_COUNT_KEY, columnCount);
        // values packing and serialization
        if (COMPRESSION)
            encodeObjectArray(serial, COMPRESSED_VALUES_KEY, RepeatedValuesCompressor.SINGLETON.compress(rs.getValues()));
        else
            encodeObjectArray(serial, VALUES_KEY, rs.getValues());
        encodeInteger(serial, VERSION_KEY, rs.getVersionNumber());
    }

    @Override
    public QueryResult decode(ReadOnlyAstObject serial) {
        // Values deserialization
        Object[] inlineValues = decodeObjectArray(serial, VALUES_KEY); // trying uncompressed values
        if (inlineValues == null) // means compressed values
            inlineValues = RepeatedValuesCompressor.SINGLETON.uncompress(decodeObjectArray(serial, COMPRESSED_VALUES_KEY));
        // returning the query result with its version number (if provided)
        QueryResult rs = new QueryResult(
                decodeInteger(serial, COLUMN_COUNT_KEY),
                inlineValues,
                decodeStringArray(serial, COLUMN_NAMES_KEY));
        rs.setVersionNumber(decodeInteger(serial, VERSION_KEY, 0));
        return rs;
    }
}
