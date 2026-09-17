package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryColumnToEntityFieldMapping;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.IdExpression;
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
        // `fk.id` join prune: the referenced row's primary key IS the foreign-key column itself, so
        // no join to the target table is needed — emit the FK column with the foreign-class mapping,
        // exactly like a bare FK select compiles with readForeignFields=false. Also fixes
        // `order by fk.id` silently dropping rows through the inner join on a nullable FK.
        // Semantics note: `fk.id is null` now tests the FK column (true for null FKs) instead of the
        // former never-true inner-join form, and dangling FKs (no referential integrity) are no
        // longer filtered out — both changes in the direction of correctness.
        if (leftSqlColumnName != null && asAlias == null && isIdTerm(dot.getRight())) {
            o.build.addColumnInClause(leftTableAlias, leftSqlColumnName, left, rightClass, o.clause, o.separator, o.grouped, false, o.generateQueryMapping);
            return;
        }
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
        // scalarContext: inside a scalar select expression the foreign key must NOT be emitted as
        // an extra column — it would be concatenated into the middle of the expression text
        if (o.isTopLevelSelect() && !o.scalarContext && leftSql != null && dot.isReadLeftKey() && o.readForeignFields) // lecture de la clé étrangère pour pouvoir faire la jointure en mémoire
            leftJoinMapping = o.build.addColumnInClause(leftTableAlias, leftSql, left, rightClass, o.clause, o.separator, o.grouped, false, o.generateQueryMapping);
        o.build.setCompilingClass(rightClass);
        o.build.setCompilingTableAlias(rightTableAlias);
        QueryColumnToEntityFieldMapping oldLeftJoinMapping = o.build.getLeftJoinMapping();
        o.build.setLeftJoinMapping(leftJoinMapping);
        Expression<?> right = dot.getRight();
        // scalarContext: a dot inside a scalar select expression compiles as a single scalar value
        // — never decomposed into separately-loaded persistent field columns
        if (o.isTopLevelSelect() && !o.scalarContext && o.separator != null && (!(right instanceof Symbol) || (((Symbol<?>) right).getExpression() != null && !o.compileExpressions)))
            compileExpressionPersistentTermsToSql(right, o);
        else
            compileChildExpressionToSql(right, o);
        o.build.setLeftJoinMapping(oldLeftJoinMapping);
        o.build.setCompilingClass(leftClass);
        o.build.setCompilingTableAlias(leftTableAlias);
    }

    /**
     * True when the expression denotes the primary key: either the IdExpression itself, or the
     * domain model's `id` field symbol (DomainClassBuilder wires that symbol's expression to
     * IdExpression.singleton).
     */
    private static boolean isIdTerm(Expression<?> e) {
        return e instanceof IdExpression
            || e instanceof Symbol<?> symbol && symbol.getExpression() instanceof IdExpression;
    }

    @Override
    protected void compileExpressionPersistentTermsToSql(Expression e, Options o) {
        // When compileExpressions is true and we have a grouped notation like accountPerson.(id, fullName),
        // we need to handle each child individually: expression fields (like fullName) go through
        // compileChildExpressionToSql (which triggers SymbolSqlCompiler's compile-to-SQL-with-alias branch),
        // while plain persistent fields (like id) use the original persistent terms path that correctly
        // handles PK field mappings via readForeignFields.
        if (o.compileExpressions && e instanceof ExpressionArray) {
            for (Expression<?> child : ((ExpressionArray<?>) e).getExpressions()) {
                if (child instanceof Symbol<?> symbol && symbol.getExpression() != null)
                    compileChildExpressionToSql(child, o);
                else
                    super.compileExpressionPersistentTermsToSql(child, o);
            }
        } else if (o.compileExpressions && e instanceof Dot) {
            // For nested Dot paths (e.g., accountPerson.fullName inside accountPerson.accountPerson.fullName),
            // compile directly so the inner DotSqlCompiler can recurse and eventually reach
            // SymbolSqlCompiler's branch (3) for expression fields like fullName.
            // Using super would decompose fullName into its persistent terms via collectPersistentTerms.
            compileChildExpressionToSql(e, o);
        } else {
            super.compileExpressionPersistentTermsToSql(e, o);
        }
    }
}
