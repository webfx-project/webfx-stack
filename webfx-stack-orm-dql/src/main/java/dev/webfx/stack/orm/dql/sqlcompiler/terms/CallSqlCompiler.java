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
public final class CallSqlCompiler extends AbstractTermSqlCompiler<Call<?>> {

    public CallSqlCompiler() {
        super(Call.class);
    }

    @Override
    public void compileExpressionToSql(Call<?> call, Options o) {
        Expression<?> arg = call.getOperand();
        if (arg instanceof Dot<?> dot) {
            compileChildExpressionToSql(Dot.dot(dot.getLeft(), new Call(call.getFunctionName(), dot.getRight(), call.getOrderBy(), call.isDistinct()), dot.isOuterJoin(), false), o);
        } else {
            Function<?> f = call.getFunction();
            if (f instanceof InlineFunction<?> inlineFunction) {
                if (o.clause == SqlClause.SELECT && o.readForeignFields)
                    compileExpressionPersistentTermsToSql(arg, o);
                else
                    try {
                        inlineFunction.pushArguments(arg);
                        compileChildExpressionToSql(inlineFunction.getBody(), o);
                    } finally {
                        inlineFunction.popArguments();
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
                    // Emit the `distinct` keyword for aggregate functions parsed
                    // as `funcName(distinct expr)` — typically `count(distinct …)`.
                    // SqlBuild.prepareAppend would otherwise prepend the "," separator
                    // before the first arg because the buffer would end with a space.
                    // Insert a transient "(" sentinel right after `distinct ` so
                    // prepareAppend sees an "open delimiter" context and skips the
                    // separator; remove the sentinel after the operand has emitted.
                    int distinctSentinelPos = -1;
                    if (call.isDistinct()) {
                        sb.append("distinct ");
                        distinctSentinelPos = sb.length();
                        sb.append('(');
                    }
                    if (arg != null)
                        compileChildExpressionToSql(arg, o.changeSeparatorGroupedGenerateQueryMapping(",", false, false).changeReadForeignFields(o.readForeignFields && f.isEvaluable()));
                    if (distinctSentinelPos >= 0)
                        sb.deleteCharAt(distinctSentinelPos);
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
