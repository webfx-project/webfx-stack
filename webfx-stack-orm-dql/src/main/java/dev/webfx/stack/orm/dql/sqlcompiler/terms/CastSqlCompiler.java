package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.Cast;

/**
 * @author Bruno Salmon
 */
public final class CastSqlCompiler extends AbstractTermSqlCompiler<Cast<?>> {

    public CastSqlCompiler() {
        super(Cast.class);
    }

    @Override
    public void compileExpressionToSql(Cast<?> e, Options o) {
        compileChildExpressionToSql(e.getOperand(), o);
        o.build.prepareAppend(o.clause, null).append("::").append(e.getCastType());
    }
}
