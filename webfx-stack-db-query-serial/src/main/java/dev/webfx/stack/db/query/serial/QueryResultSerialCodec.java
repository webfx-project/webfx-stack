package dev.webfx.stack.db.query.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.query.CompressionMetrics;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.serial.compression.repeat.RepeatedValuesCompressor;

public final class QueryResultSerialCodec extends SerialCodecBase<QueryResult> {

    public static boolean COMPRESSION = true; // Not final as this flag is turned off by the kbs2-model-import module to make the domain model snapshot
    private final static String CODEC_ID = "QueryResult";
    private final static String COLUMN_NAMES_KEY = "columnNames";
    private final static String COLUMN_COUNT_KEY = "columnCount";
    private final static String VALUES_KEY = "values";
    private final static String COMPRESSED_VALUES_KEY = "cvalues";
    private final static String VERSION_KEY = "version";
    private final static String ENTITY_MAPPING_KEY = "entityMapping";
    private final static String CALL_SEQ_KEY = "callSeq";

    public QueryResultSerialCodec() {
        super(QueryResult.class, CODEC_ID);
    }

    @Override
    public void encode(QueryResult rs, AstObject serial) {
        int columnCount = rs.getColumnCount();
        encodeStringArray(serial, COLUMN_NAMES_KEY, rs.getColumnNames());
        encodeInteger(serial, COLUMN_COUNT_KEY, columnCount);
        // values packing and serialization
        if (COMPRESSION) {
            // Time the compression: it runs inline on the Vert.x event loop, so its cost is a
            // per-result blocking risk we surface on /monitor (see CompressionMetrics).
            Object[] values = rs.getValues();
            long t0 = System.nanoTime();
            Object[] compressed = RepeatedValuesCompressor.SINGLETON.compress(values);
            CompressionMetrics.record(System.nanoTime() - t0, values == null ? 0 : values.length);
            encodeObjectArray(serial, COMPRESSED_VALUES_KEY, compressed);
        } else
            encodeObjectArray(serial, VALUES_KEY, rs.getValues());
        encodeInteger(serial, VERSION_KEY, rs.getVersionNumber());
        encodeObject(serial, ENTITY_MAPPING_KEY, rs.getEntityMapping());
        encodeInteger(serial, CALL_SEQ_KEY, rs.getCallSeq(), 0);
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
        rs.setEntityMapping(decodeObject(serial, ENTITY_MAPPING_KEY));
        rs.setCallSeq(decodeInteger(serial, CALL_SEQ_KEY, 0));
        return rs;
    }
}
