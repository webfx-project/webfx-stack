package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.terms.ParameterReference;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;

/**
 * @author Bruno Salmon
 */
public final class ParameterReferenceSqlCompiler extends AbstractTermSqlCompiler<ParameterReference<?>> {

    public ParameterReferenceSqlCompiler() {
        super(ParameterReference.class);
    }

    @Override
    public void compileExpressionToSql(ParameterReference p, Options o) {
        compileParameter(p, o, true);
    }

    public void compileParameter(ParameterReference p, Options o, boolean isRightOperand) {
        int index = p.getIndex();
        String name = p.getName();
        if (index <= 0 && name != null && !p.isSearchParameter()) {
            if (p.isClientOnlyParameter(o.clause == SqlClause.SELECT)) // TODO: distinguish sql parameters from local parameters
                return;
            if (p.getRightDot() != null)
                o.build.setCacheable(false);
            // Each parameter name is mapped to a parameter index, which is its position in the parameter list (+1)
            index = o.build.getParameterNames().indexOf(name) + 1; // reusing existing index if name has been used before
            if (index <= 0) { // otherwise increasing the parameter index
                o.build.getParameterNames().add(name);
                index = o.build.getParameterNames().size();
            }
            /*
            int parameterIndex = e.getIndex() != -1 ? e.getIndex() : o.build.getParameterCount();
            Object parameterValue = o.parameterValues[parameterIndex];
            if (parameterValue instanceof ParameterJoinValue) {
                ParameterJoinValue value = (ParameterJoinValue) parameterValue;
                Object previousCompilingClass = o.build.getCompilingClass();
                String previousTableAlias = o.build.getCompilingTableAlias();
                Object parameterClass = previousCompilingClass.getDomainModel().getClass(value.getId().getClassId());
                String tableAlias = o.build.getNewTableAlias(o.modelReader.getDomainClassSqlTableName(parameterClass), null, false);
                Expression right = Parser.parseExpression(value.getRightDot(), parameterClass);
                o.build.setCompilingClass(parameterClass);
                o.build.setCompilingTableAlias(tableAlias);
                boolean lowerRightPrecedence = right.getPrecedenceLevel() < 8; // DOT
                // assuming it's a right value ex: x=?p.y => compiled to x=p.y and p.id=? (left value (ex: p.y=null) is not well compiled!...
                if (isRightOperand) {
                    StringBuilder sb =  o.build.prepareAppend(o);
                    if (lowerRightPrecedence)
                        sb.append('(');
                    compileChildExpressionToSql(right, o.changeSeparator(null));
                    if (lowerRightPrecedence)
                        sb.append(')');
                    sb = o.build.prepareAppend(SqlClause.WHERE, " and ");
                    sb.append(tableAlias).append('.').append(o.modelReader.getDomainClassPrimaryKeySqlColumnName(parameterClass)).append("=?");
                } else {
                    StringBuilder sb =  o.build.prepareAppend(o);
                    sb.append(tableAlias).append('.').append(o.modelReader.getDomainClassPrimaryKeySqlColumnName(parameterClass)).append("=?");
                    sb = o.build.prepareAppend(SqlClause.WHERE, " and ");
                    if (lowerRightPrecedence)
                        sb.append('(');
                    compileChildExpressionToSql(right, o.changeSeparator(null));
                    if (lowerRightPrecedence)
                        sb.append(')');
                }
                o.build.setCompilingClass(parameterClass);
                o.build.setCompilingTableAlias(previousTableAlias);
                return;
            }
            */
        }
        if (index <= 0) // happens with `?` parameters (no name, no index)
            index = o.build.incrementParameterIndex();
        o.build.addColumnInClause(null, o.build.getDbmsSyntax().generateParameterToken(index), null, null, o.clause, o.separator, false, false, o.generateQueryMapping);
    }

}
