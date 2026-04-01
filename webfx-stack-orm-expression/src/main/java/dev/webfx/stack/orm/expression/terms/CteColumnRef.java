package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.lci.DomainWriter;

/**
 * Represents a reference to a computed column exposed by a CTE (e.g. "e.finalEvent").
 * Compiles directly to the SQL column reference "cteAlias.sqlColumnName" without
 * attempting any domain model lookup.
 *
 * @author Bruno Salmon
 */
public final class CteColumnRef<T> extends AbstractExpression<T> {

    private final String cteAlias;     // e.g. "e"
    private final String fieldName;    // camelCase field name, e.g. "finalEvent"

    public CteColumnRef(String cteAlias, String fieldName) {
        super(10); // same precedence level as Symbol
        this.cteAlias = cteAlias;
        this.fieldName = fieldName;
    }

    public String getCteAlias() {
        return cteAlias;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        return domainReader.getDomainFieldValue(domainObject, fieldName);
    }

    @Override
    public void setValue(T domainObject, Object value, DomainWriter<T> dataWriter) {
        // read-only CTE column — no-op
    }

    @Override
    public void collect(CollectOptions options) {
        // nothing to collect — not a persistent domain field
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        return sb.append(cteAlias).append('.').append(fieldName);
    }
}
