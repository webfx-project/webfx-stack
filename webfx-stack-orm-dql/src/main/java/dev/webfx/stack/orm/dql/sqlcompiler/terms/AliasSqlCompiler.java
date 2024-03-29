package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.function.ArgumentAlias;

/**
 * @author Bruno Salmon
 */
public final class AliasSqlCompiler extends AbstractTermSqlCompiler<Alias> {

    public AliasSqlCompiler() {
        super(Alias.class, ArgumentAlias.class);
    }

    @Override
    public void compileExpressionToSql(Alias e, Options o) {
        if (e instanceof ArgumentAlias) /***  called during inline function sql compilation ***/
            compileChildExpressionToSql((Expression) ((ArgumentAlias) e).getArgument(), o);
        else // Standard alias, called only when alone (not followed by dot since Dot manages this case), so refers implicitly to id in that case
            o.build.prepareAppend(o).append(o.build.getSqlAlias(e.getName())).append('.').append(o.modelReader.getDomainClassPrimaryKeySqlColumnName(e.getDomainClass()));
    }
}
