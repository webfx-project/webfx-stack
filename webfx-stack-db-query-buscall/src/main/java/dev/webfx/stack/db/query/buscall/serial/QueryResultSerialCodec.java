package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.stack.com.serial.SerialCodecManager;
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
    public void encodeToJson(QueryResult rs, AstObject json) {
        try {
            int columnCount = rs.getColumnCount();
            // Column names serialization
            AstArray namesArray = AST.createArray();
            String[] columnNames = rs.getColumnNames();
            if (columnNames != null) {
                for (String name : columnNames)
                    namesArray.push(name);
                json.set(COLUMN_NAMES_KEY, namesArray);
                columnCount = namesArray.size();
            }
            json.set(COLUMN_COUNT_KEY, columnCount);
            // values packing and serialization
            if (COMPRESSION)
                json.set(COMPRESSED_VALUES_KEY, SerialCodecManager.encodePrimitiveArrayToAstArray(RepeatedValuesCompressor.SINGLETON.compress(rs.getValues())));
            else
                json.set(VALUES_KEY, SerialCodecManager.encodePrimitiveArrayToAstArray(rs.getValues()));
            SerialCodecBase.encodeKey(VERSION_KEY, rs.getVersionNumber(), json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public QueryResult decodeFromJson(ReadOnlyAstObject json) {
        //Logger.log("Decoding json result set: " + json);
        Integer columnCount = json.getInteger(COLUMN_COUNT_KEY);
        // Column names deserialization
        String[] names = null;
        ReadOnlyAstArray namesArray = json.getArray(COLUMN_NAMES_KEY);
        if (namesArray != null) {
            columnCount = namesArray.size();
            names = new String[columnCount];
            for (int i = 0; i < columnCount; i++)
                names[i] = namesArray.getString(i);
        }
        // Values deserialization
        Object[] inlineValues;
        ReadOnlyAstArray valuesArray = json.getArray(VALUES_KEY);
        if (valuesArray != null)
            inlineValues = SerialCodecManager.decodePrimitiveArrayFromAstArray(valuesArray);
        else
            inlineValues = RepeatedValuesCompressor.SINGLETON.uncompress(SerialCodecManager.decodePrimitiveArrayFromAstArray(json.getArray(COMPRESSED_VALUES_KEY)));
        // returning the query result with its version number (if provided)
        QueryResult rs = new QueryResult(columnCount, inlineValues, names);
        rs.setVersionNumber(json.getInteger(VERSION_KEY, 0));
        return rs;
    }
}
