package dev.webfx.stack.orm.dql.query.interceptor.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryColumnToEntityFieldMapping;
import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryRowToEntityMapping;

/**
 * Serial codec for QueryRowToEntityMapping — encodes/decodes the row-to-entity
 * mapping used to reconstruct entities (with joins) from query results.
 *
 * Uses short keys to minimize wire size:
 *   pk = primaryKeyColumnIndex, dc = domainClassId, cm = columnMappings
 *
 * @author Bruno Salmon
 */
public final class QueryRowToEntityMappingSerialCodec extends SerialCodecBase<QueryRowToEntityMapping> {

    private static final String CODEC_ID = "RowMap";
    private static final String PRIMARY_KEY_COLUMN_INDEX_KEY = "pk";
    private static final String DOMAIN_CLASS_ID_KEY = "dc";
    private static final String COLUMN_MAPPINGS_KEY = "cm";

    public QueryRowToEntityMappingSerialCodec() {
        super(QueryRowToEntityMapping.class, CODEC_ID);
    }

    @Override
    public void encode(QueryRowToEntityMapping mapping, AstObject serial) {
        encodeInteger(serial, PRIMARY_KEY_COLUMN_INDEX_KEY, mapping.getPrimaryKeyColumnIndex());
        encodeObject(serial, DOMAIN_CLASS_ID_KEY, QueryColumnToEntityFieldMappingSerialCodec.resolveClassId(mapping.getDomainClassId()));
        encodeObjectArray(serial, COLUMN_MAPPINGS_KEY, mapping.getColumnMappings());
    }

    @Override
    public QueryRowToEntityMapping decode(ReadOnlyAstObject serial) {
        Object[] decodedMappings = decodeObjectArray(serial, COLUMN_MAPPINGS_KEY);
        QueryColumnToEntityFieldMapping[] columnMappings;
        if (decodedMappings == null) {
            columnMappings = new QueryColumnToEntityFieldMapping[0];
        } else {
            columnMappings = new QueryColumnToEntityFieldMapping[decodedMappings.length];
            for (int i = 0; i < decodedMappings.length; i++)
                columnMappings[i] = (QueryColumnToEntityFieldMapping) decodedMappings[i];
        }
        int primaryKeyColumnIndex = decodeInteger(serial, PRIMARY_KEY_COLUMN_INDEX_KEY, 0);
        Object domainClassId = decodeObject(serial, DOMAIN_CLASS_ID_KEY);
        return new QueryRowToEntityMapping(primaryKeyColumnIndex, domainClassId, columnMappings);
    }
}
