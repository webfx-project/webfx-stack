package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryColumnToEntityFieldMapping;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.stack.orm.expression.terms.function.ArgumentAlias;

/**
 * @author Bruno Salmon
 */
public final class DotSqlCompiler extends AbstractTermSqlCompiler<Dot<?>> {

    public DotSqlCompiler() {
        super(Dot.class);
    }

    @Override
    public void compileExpressionToSql(Dot<?> dot, Options o) {
        Expression<?> left = dot.getLeft();
        Object leftClass = o.build.getCompilingClass();
        String asAlias = null;
        if (left instanceof As<?> as) {
            left = as.getOperand();
            asAlias = as.getAlias();
        }
        if (left instanceof ArgumentAlias argumentAlias) // Resolving argument alias with the actual argument expression
            left = (Expression<?>) argumentAlias.getArgument();
        if (left instanceof Dot) { // the initial leftDot was not a dot, but the argument alias might have introduced one. Ex: p.frontendAccount with p = document.person
            compileExpressionToSql(Dot.dot(left, dot.getRight(), dot.isOuterJoin(), o.readForeignFields), o);
            return;
        }
        Object rightClass = o.modelReader.getSymbolForeignDomainClass(leftClass, left, true); // was e.getType().getForeignClass();
        final String leftTableAlias = o.build.getCompilingTableAlias();
        final String leftSql;
        final String rightTableAlias;
        String leftSqlColumnName = o.modelReader.getSymbolSqlColumnName(leftClass, left);
        if (leftSqlColumnName != null) { // typically a persistent field
            leftSql = leftSqlColumnName;
            rightTableAlias = o.build.addJoinCondition(leftTableAlias, leftSql, asAlias, o.modelReader.getDomainClassSqlTableName(rightClass), o.modelReader.getDomainClassPrimaryKeySqlColumnName(rightClass), dot.isOuterJoin() || o.clause == SqlClause.SELECT);
        } else if (left instanceof Alias<?> alias) {
            leftSql = null;
            rightClass = alias.getDomainClass();
            rightTableAlias = alias.getName();
        } else // should never occur
            leftSql = rightTableAlias = null;
        QueryColumnToEntityFieldMapping leftJoinMapping = null;
        if (o.isTopLevelSelect() && leftSql != null && dot.isReadLeftKey() && o.readForeignFields) // lecture de la clé étrangère pour pouvoir faire la jointure en mémoire
            leftJoinMapping = o.build.addColumnInClause(leftTableAlias, leftSql, left, rightClass, o.clause, o.separator, o.grouped, false, o.generateQueryMapping);
        o.build.setCompilingClass(rightClass);
        o.build.setCompilingTableAlias(rightTableAlias);
        QueryColumnToEntityFieldMapping oldLeftJoinMapping = o.build.getLeftJoinMapping();
        o.build.setLeftJoinMapping(leftJoinMapping);
        Expression<?> right = dot.getRight();
        if (o.isTopLevelSelect() && o.separator != null && (!(right instanceof Symbol) || ((Symbol<?>) right).getExpression() != null))
            compileExpressionPersistentTermsToSql(right, o);
        else
            compileChildExpressionToSql(right, o);
        o.build.setLeftJoinMapping(oldLeftJoinMapping);
        o.build.setCompilingClass(leftClass);
        o.build.setCompilingTableAlias(leftTableAlias);
    }
}
