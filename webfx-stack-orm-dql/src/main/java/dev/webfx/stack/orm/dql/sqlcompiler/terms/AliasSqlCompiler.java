package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.extras.type.Type;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.function.ArgumentAlias;
import dev.webfx.stack.orm.expression.terms.function.DomainClassType;

/**
 * @author Bruno Salmon
 */
public final class AliasSqlCompiler extends AbstractTermSqlCompiler<Alias> {

    public AliasSqlCompiler() {
        super(Alias.class, ArgumentAlias.class);
    }

    @Override
    public void compileExpressionToSql(Alias e, Options o) {
        if (e instanceof ArgumentAlias) // called during inline function SQL compilation
            compileChildExpressionToSql((Expression<?>) ((ArgumentAlias) e).getArgument(), o);
        else { // Standard alias, called only when alone (not followed by dot since Dot manages this case)
            // We append the alias name to SQL
            StringBuilder sb = o.build.prepareAppend(o).append(o.build.getSqlAlias(e.getName()));
            // And if the alias actually refers to a domain class (ex: DocumentLine dl), we add the primary key
            // column name (ex: DQL "order by dl" will be translated to SQL "order by dl.id")
            Type type = e.getType();
            if (type == null || type instanceof DomainClassType) // null type also indicates this case
                sb.append('.').append(o.modelReader.getDomainClassPrimaryKeySqlColumnName(e.getDomainClass()));
            // However, for other types, we leave it like this. For example, if the alias comes from a subquery
            // such as DQL "exists(select ...) as present", then we translate DQL "order by present" to SQL
            // "order by present" (and not "order by present.id") because e.getType() = PrimType.BOOLEAN.
        }
    }
}
