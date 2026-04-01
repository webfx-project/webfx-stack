package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.CteColumnRef;

/**
 * Compiles a CteColumnRef to its SQL column reference: "cteAlias.sql_column_name".
 *
 * @author Bruno Salmon
 */
public final class CteColumnRefSqlCompiler extends AbstractTermSqlCompiler<CteColumnRef> {

    public CteColumnRefSqlCompiler() {
        super(CteColumnRef.class);
    }

    @Override
    public void compileExpressionToSql(CteColumnRef e, Options o) {
        // CTE column aliases are user-defined names, not domain model field names — no snake_case conversion
        o.build.prepareAppend(o)
                .append(o.build.getSqlAlias(e.getCteAlias()))
                .append('.')
                .append(e.getFieldName());
    }
}
