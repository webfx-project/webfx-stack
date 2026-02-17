package dev.webfx.stack.orm.dql.query.interceptor.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryColumnToEntityFieldMapping;
import dev.webfx.stack.orm.expression.terms.IdExpression;

/**
 * Serial codec for QueryColumnToEntityFieldMapping — encodes/decodes the per-column
 * entity field mapping used to reconstruct entities from query results.
 *
 * During encoding, DomainField and DomainClass objects are resolved to their simple IDs
 * so the client receives plain values (strings or numbers) instead of complex objects.
 *
 * Uses short keys to minimize wire size:
 *   ci = columnIndex, fi = domainFieldId, fc = foreignClassId, fk = foreignIdColumnMapping
 *
 * @author Bruno Salmon
 */
public final class QueryColumnToEntityFieldMappingSerialCodec extends SerialCodecBase<QueryColumnToEntityFieldMapping> {

    private static final String CODEC_ID = "ColMap";
    private static final String COLUMN_INDEX_KEY = "ci";
    private static final String DOMAIN_FIELD_ID_KEY = "fi";
    private static final String FOREIGN_CLASS_ID_KEY = "fc";
    private static final String FOREIGN_ID_COLUMN_MAPPING_KEY = "fk";

    public QueryColumnToEntityFieldMappingSerialCodec() {
        super(QueryColumnToEntityFieldMapping.class, CODEC_ID);
    }

    @Override
    public void encode(QueryColumnToEntityFieldMapping mapping, AstObject serial) {
        encodeInteger(serial, COLUMN_INDEX_KEY, mapping.getColumnIndex());
        encodeObject(serial, DOMAIN_FIELD_ID_KEY, resolveFieldId(mapping.getDomainFieldId()));
        if (mapping.getForeignClassId() != null)
            encodeObject(serial, FOREIGN_CLASS_ID_KEY, resolveClassId(mapping.getForeignClassId()));
        if (mapping.getForeignIdColumnMapping() != null)
            encodeObject(serial, FOREIGN_ID_COLUMN_MAPPING_KEY, mapping.getForeignIdColumnMapping());
    }

    @Override
    public QueryColumnToEntityFieldMapping decode(ReadOnlyAstObject serial) {
        int columnIndex = decodeInteger(serial, COLUMN_INDEX_KEY, 0);
        Object domainFieldId = decodeObject(serial, DOMAIN_FIELD_ID_KEY);
        Object foreignClassId = decodeObject(serial, FOREIGN_CLASS_ID_KEY);
        QueryColumnToEntityFieldMapping foreignIdColumnMapping = decodeObject(serial, FOREIGN_ID_COLUMN_MAPPING_KEY);
        return new QueryColumnToEntityFieldMapping(columnIndex, domainFieldId, foreignClassId, foreignIdColumnMapping);
    }

    /** Resolve a domainFieldId to a simple value: DomainField → its id, otherwise kept as-is. */
    static Object resolveFieldId(Object fieldId) {
        if (fieldId instanceof DomainField domainField)
            return domainField.getId();
        if (fieldId instanceof IdExpression<?>)
            return "id";
        return fieldId;
    }

    /** Resolve a classId to a simple value: DomainClass → its id, otherwise kept as-is. */
    static Object resolveClassId(Object classId) {
        if (classId instanceof DomainClass)
            return ((DomainClass) classId).getId();
        return classId;
    }
}
