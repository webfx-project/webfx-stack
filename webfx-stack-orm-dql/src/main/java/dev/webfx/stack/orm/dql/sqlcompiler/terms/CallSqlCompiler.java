package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.function.Call;
import dev.webfx.stack.orm.expression.terms.function.Function;
import dev.webfx.stack.orm.expression.terms.function.InlineFunction;

/**
 * @author Bruno Salmon
 */
public final class CallSqlCompiler extends AbstractTermSqlCompiler<Call> {

    public CallSqlCompiler() {
        super(Call.class);
    }

    @Override
    public void compileExpressionToSql(Call call, Options o) {
        Expression arg = call.getOperand();
        if (arg instanceof Dot) {
            Dot dot = (Dot) arg;
            compileChildExpressionToSql(Dot.dot(dot.getLeft(), new Call(call.getFunctionName(), dot.getRight(), call.getOrderBy()), dot.isOuterJoin(), false), o);
        } else {
            Function f = call.getFunction();
            if (f instanceof InlineFunction) {
                InlineFunction inline = (InlineFunction) f;
                if (o.clause == SqlClause.SELECT && o.readForeignFields)
                    compileExpressionPersistentTermsToSql(arg, o);
                else
                    try {
                        inline.pushArguments(arg);
                        compileChildExpressionToSql(inline.getBody(), o);
                    } finally {
                        inline.popArguments();
                    }
            } else {
                StringBuilder sb;
                String name = ExpressionSqlCompiler.toSqlString(f.getName()); // Ex: AbcNames transformed to abc_names
                if (o.generateQueryMapping) {
                    o.build.addColumnInClause(null, name, name, null, o.clause, o.separator, false, false, true);
                    sb = o.build.prepareAppend(o.clause, "");
                } else
                    sb = o.build.prepareAppend(o).append(name);
                if (!f.isKeyword()) {
                    sb.append('(');
                    if (arg != null)
                        compileChildExpressionToSql(arg, o.changeSeparatorGroupedGenerateQueryMapping(",", false, false).changeReadForeignFields(o.readForeignFields && f.isEvaluable()));
                    if (call.getOrderBy() != null) {
                        sb.append(" order by ");
                        compileChildExpressionToSql(call.getOrderBy(), o.changeSeparatorGroupedGenerateQueryMapping(",", false, false));
                    }
                    sb.append(')');
                }
            }
        }
    }
}
