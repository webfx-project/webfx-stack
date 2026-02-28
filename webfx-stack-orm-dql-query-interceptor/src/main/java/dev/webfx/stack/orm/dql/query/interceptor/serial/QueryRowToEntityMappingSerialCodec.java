package dev.webfx.stack.orm.dql.query.interceptor.serial;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstArray;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
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
        // Compact column mappings: no $codec/ci per entry.
        // String → simple field, Array → [fi, fc] or [fi, fc|null, fk]
        QueryColumnToEntityFieldMapping[] mappings = mapping.getColumnMappings();
        if (mappings != null && mappings.length > 0) {
            AstArray cmArray = AST.createArray();
            for (QueryColumnToEntityFieldMapping m : mappings) {
                Object fieldId = QueryColumnToEntityFieldMappingSerialCodec.resolveFieldId(m.getDomainFieldId());
                if (m.getForeignClassId() == null && m.getForeignIdColumnMapping() == null) {
                    // Simple field → just the field name string
                    cmArray.push(fieldId);
                } else {
                    // Complex field → array: [fi, fc] or [fi, fc|null, fk]
                    AstArray entry = AST.createArray();
                    entry.push(fieldId);
                    if (m.getForeignIdColumnMapping() != null) {
                        // [fi, fc|null, fk]
                        Object fc = m.getForeignClassId() != null ? QueryColumnToEntityFieldMappingSerialCodec.resolveClassId(m.getForeignClassId()) : null;
                        entry.push(fc);
                        entry.push(m.getForeignIdColumnMapping().getColumnIndex());
                    } else {
                        // [fi, fc]
                        entry.push(QueryColumnToEntityFieldMappingSerialCodec.resolveClassId(m.getForeignClassId()));
                    }
                    cmArray.push(entry);
                }
            }
            serial.set(COLUMN_MAPPINGS_KEY, cmArray);
        }
    }

    @Override
    public QueryRowToEntityMapping decode(ReadOnlyAstObject serial) {
        int primaryKeyColumnIndex = decodeInteger(serial, PRIMARY_KEY_COLUMN_INDEX_KEY, 0);
        Object domainClassId = decodeObject(serial, DOMAIN_CLASS_ID_KEY);
        ReadOnlyAstArray cmArray = serial.getArray(COLUMN_MAPPINGS_KEY);
        QueryColumnToEntityFieldMapping[] columnMappings;
        if (cmArray == null || cmArray.size() == 0) {
            columnMappings = new QueryColumnToEntityFieldMapping[0];
        } else {
            int n = cmArray.size();
            columnMappings = new QueryColumnToEntityFieldMapping[n];
            for (int i = 0; i < n; i++) {
                int columnIndex = i + 1; // ci inferred from array position (pk occupies column 0)
                if (cmArray.isScalar(i)) {
                    // Simple field: just the field name string
                    Object fieldId = cmArray.getElement(i);
                    columnMappings[i] = new QueryColumnToEntityFieldMapping(columnIndex, fieldId);
                } else if (cmArray.isArray(i)) {
                    // Complex field: array [fi, fc] or [fi, fc|null, fk]
                    ReadOnlyAstArray entry = cmArray.getArray(i);
                    Object fieldId = entry.getElement(0);
                    if (entry.size() == 3) {
                        // [fi, fc|null, fk]
                        Object foreignClassId = entry.getElement(1); // may be null
                        int fkIndex = entry.getInteger(2);
                        QueryColumnToEntityFieldMapping foreignIdColumnMapping = columnMappings[fkIndex - 1];
                        columnMappings[i] = new QueryColumnToEntityFieldMapping(columnIndex, fieldId, foreignClassId, foreignIdColumnMapping);
                    } else {
                        // [fi, fc]
                        Object foreignClassId = entry.getElement(1);
                        columnMappings[i] = new QueryColumnToEntityFieldMapping(columnIndex, fieldId, foreignClassId, null);
                    }
                }
            }
        }
        return new QueryRowToEntityMapping(primaryKeyColumnIndex, domainClassId, columnMappings);
    }
}
