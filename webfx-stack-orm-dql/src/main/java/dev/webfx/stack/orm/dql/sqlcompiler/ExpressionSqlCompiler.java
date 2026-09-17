package dev.webfx.stack.orm.dql.sqlcompiler;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlBuild;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.DbmsSqlSyntax;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.HsqlSyntax;
import dev.webfx.stack.orm.dql.sqlcompiler.terms.*;
import dev.webfx.stack.orm.expression.terms.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class ExpressionSqlCompiler {

    private static final Map<Class<? extends Expression>, AbstractTermSqlCompiler<?>> termCompilers = new HashMap<>();

    static {
        // Registering all term compilers (some can actually compile several term classes).
        registerTermCompilers(
                new ExpressionArraySqlCompiler(),    // ExpressionArray
                new BinaryExpressionSqlCompiler(),   // Divide, Minus, Multiply, Plus
                new BooleanExpressionSqlCompiler(),  // And, Equals, GreaterThan, GreaterThanOrEquals, In, LessThan, LessThanOrEquals, Like, NotEquals, NotLike, Or, All, Any
                new AliasSqlCompiler(),              // Alias, ArgumentAlias
                new CallSqlCompiler(),               // Call
                new DotSqlCompiler(),                // Dot
                new SelectExpressionSqlCompiler(),   // SelectExpression, Exists
                new TernaryExpressionSqlCompiler(),  // TernaryExpression
                new UnaryExpressionSqlCompiler(),    // Array, As, Not
                new OrderedSqlCompiler(),            // Ordered
                new ConstantSqlCompiler(),           // Constant
                new ParameterReferenceSqlCompiler(), // ParameterReference
                new CastSqlCompiler(),               // Cast
                new IdSqlCompiler(),                 // IdExpression
                new CteColumnRefSqlCompiler(),       // CteColumnRef
                new SymbolSqlCompiler()              // Symbol (then extendable using)
        );
    }

    private static void registerTermCompilers(AbstractTermSqlCompiler<?>... termSqlCompilers) {
        for (AbstractTermSqlCompiler<?> expressionSqlCompiler : termSqlCompilers)
            for (Class<? extends Expression> expressionClass : expressionSqlCompiler.getSupportedTermClasses())
                termCompilers.put(expressionClass, expressionSqlCompiler);
    }

    @SafeVarargs
    public static <T extends Expression> void declareCompilableSubclasses(Class<T> superClass, Class<? extends T>... subclasses) {
        AbstractTermSqlCompiler<?> superCompiler = termCompilers.get(superClass);
        for (Class<? extends T> subclass : subclasses)
            termCompilers.put(subclass, superCompiler);
    }

    @SafeVarargs
    public static void declareSymbolSubclasses(Class<? extends Symbol>... symbolSubclasses) {
        declareCompilableSubclasses(Symbol.class, symbolSubclasses);
    }

    /*** Public entry points ***/

    public static SqlCompiled compileStatement(DqlStatement statement, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        if (statement instanceof WithSelect)
            return compileWithSelect((WithSelect) statement, dbmsSyntax, modelReader);
        if (statement instanceof Union)
            return compileUnion((Union) statement, dbmsSyntax, false, false, false, modelReader);
        if (statement instanceof Insert)
            return compileInsert((Insert) statement, dbmsSyntax, modelReader);
        if (statement instanceof Update)
            return compileUpdate((Update) statement, dbmsSyntax, modelReader);
        if (statement instanceof Delete)
            return compileDelete((Delete) statement, dbmsSyntax, modelReader);
        if (statement instanceof Select)
            return compileSelect((Select) statement, dbmsSyntax, false, false, modelReader);
        return null;
    }

    public static SqlCompiled compileWithSelect(WithSelect withSelect, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        return compileWithSelect(withSelect, dbmsSyntax, false, false, false, modelReader);
    }

    public static SqlCompiled compileWithSelect(WithSelect withSelect, DbmsSqlSyntax dbmsSyntax, boolean generateQueryMapping, boolean readForeignFields, boolean compileExpressions, CompilerDomainModelReader modelReader) {
        // Compile each CTE and build the WITH prefix
        StringBuilder withPrefix = new StringBuilder("with ");
        List<String> allParamNames = new ArrayList<>();
        boolean first = true;
        for (Object cteEntry : withSelect.getCtes()) {
            Object[] cte = (Object[]) cteEntry;
            if (!first) withPrefix.append(", ");
            String cteAlias = (String) cte[0];
            Select<?> cteSelect = (Select<?>) cte[1];
            SqlCompiled cteCompiled = compileSelect(cteSelect, dbmsSyntax, false, false, false, modelReader);
            // AS MATERIALIZED forces Postgres to compute the CTE once: PG12+ inlines
            // single-reference CTEs by default, re-executing the body inside any correlated
            // subquery that references it (which defeats a precomputation CTE entirely).
            withPrefix.append(cteAlias).append(cte.length > 2 && Boolean.TRUE.equals(cte[2]) ? " as materialized (" : " as (").append(cteCompiled.getSql()).append(")");
            // Merge parameter names (preserving order, deduplicating)
            for (String param : cteCompiled.getParameterNames())
                if (!allParamNames.contains(param))
                    allParamNames.add(param);
            first = false;
        }
        withPrefix.append(' ');
        // Compile the main select with the same flags as a regular select
        SqlCompiled mainCompiled = compileSelect(withSelect.getMainSelect(), dbmsSyntax, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
        // Merge main select parameter names
        for (String param : mainCompiled.getParameterNames())
            if (!allParamNames.contains(param))
                allParamNames.add(param);
        // Combine WITH prefix + main SQL
        String combinedSql = withPrefix.toString() + mainCompiled.getSql();
        return new SqlCompiled(combinedSql, mainCompiled.getCountSql(), allParamNames, true,
                null, mainCompiled.getQueryMapping(), mainCompiled.getSqlUncompilableCondition(), mainCompiled.isCacheable());
    }

    // Union compilation

    public static SqlCompiled compileUnion(Union union, DbmsSqlSyntax dbmsSyntax, boolean generateQueryMapping, boolean readForeignFields, boolean compileExpressions, CompilerDomainModelReader modelReader) {
        // Every branch is compiled with the SAME flags so they all emit the same column list
        // (readForeignFields in particular expands foreign display fields into extra columns);
        // the first branch's query mapping then applies to every row of the union result.
        // The first branch's SqlBuild is kept because a union-level order by resolves against it.
        SqlBuild firstBuild = buildSelect(union.getFirstSelect(), dbmsSyntax, generateQueryMapping, readForeignFields, compileExpressions, null, null, modelReader);
        SqlCompiled firstCompiled = firstBuild.toSqlCompiled(); // freezes the first branch's SQL
        // Branches are parenthesized so a per-branch order by/limit/offset remains valid SQL
        StringBuilder sql = new StringBuilder("(").append(firstCompiled.getSql()).append(')');
        List<String> allParamNames = new ArrayList<>(firstCompiled.getParameterNames());
        boolean cacheable = firstCompiled.isCacheable();
        for (Object unionEntryObj : union.getUnions()) {
            Object[] unionEntry = (Object[]) unionEntryObj;
            boolean unionAll = (Boolean) unionEntry[0];
            Select<?> branchSelect = (Select<?>) unionEntry[1];
            SqlCompiled branchCompiled = compileSelect(branchSelect, dbmsSyntax, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
            sql.append(unionAll ? " union all (" : " union (").append(branchCompiled.getSql()).append(')');
            // Merge named parameters (preserving order, deduplicating) — positional $N parameters
            // keep their index across branches, so they need no merging
            for (String param : branchCompiled.getParameterNames())
                if (!allParamNames.contains(param))
                    allParamNames.add(param);
            cacheable &= branchCompiled.isCacheable();
        }
        if (union.getOrderBy() != null)
            compileUnionOrderBy(union.getOrderBy(), firstBuild, sql.append(" order by "), modelReader);
        return new SqlCompiled(sql.toString(), null, allParamNames, true,
                null, firstCompiled.getQueryMapping(), firstCompiled.getSqlUncompilableCondition(), cacheable);
    }

    /**
     * Compiles the union-level order by. SQL restricts a set-operation order by to OUTPUT columns
     * (names or ordinals) — arbitrary expressions are rejected by Postgres there. So each key is
     * resolved against the first branch's select columns:
     * - a key naming a select-list 'as' alias is emitted as that alias (an output column name);
     * - any other key expression is scratch-compiled in the first branch's context and looked up
     *   among the first branch's select columns, then emitted as the matching column's ordinal.
     * A key matching no select column is an error: the caller must add the expression to the
     * select list of every branch (usually under an 'as' alias) before ordering by it.
     */
    private static void compileUnionOrderBy(ExpressionArray<?> orderBy, SqlBuild firstBuild, StringBuilder sql, CompilerDomainModelReader modelReader) {
        List<String> selectColumns = firstBuild.getSelectColumns();
        boolean first = true;
        for (Expression<?> key : orderBy.getExpressions()) {
            if (!first)
                sql.append(", ");
            first = false;
            Expression<?> operand = key;
            Ordered<?> ordered = key instanceof Ordered ? (Ordered<?>) key : null;
            if (ordered != null)
                operand = ordered.getOperand();
            if (operand instanceof Alias) { // a select-list 'as' alias reference => an output column name
                String aliasName = ((Alias<?>) operand).getName();
                if (selectColumns.stream().noneMatch(column -> column.endsWith(" as " + aliasName)))
                    throw new IllegalArgumentException("Union-level order by alias '" + aliasName + "' is not a select column alias of the first branch");
                sql.append(aliasName);
            } else { // any other expression => must match a select column, emitted as its ordinal
                String operandSql = firstBuild.compileToScratchSqlText(operand, modelReader);
                int ordinal = 0;
                for (int i = 0; i < selectColumns.size(); i++) {
                    String column = selectColumns.get(i);
                    // a column may carry an ' as <alias>' suffix on top of the key expression
                    if (column.equals(operandSql) || column.startsWith(operandSql) && column.substring(operandSql.length()).matches(" as \\w+")) {
                        ordinal = i + 1;
                        break;
                    }
                }
                if (ordinal == 0)
                    throw new IllegalArgumentException("Union-level order by keys must reference selected columns (SQL restriction on set operations) — add '" + operand + "' to the select list of every branch (e.g. under an 'as' alias)");
                sql.append(ordinal);
            }
            if (ordered != null) {
                if (ordered.isAscending())
                    sql.append(" asc");
                else if (ordered.isDescending())
                    sql.append(" desc");
                if (ordered.isNullsFirst())
                    sql.append(" nulls first");
                else if (ordered.isNullsLast())
                    sql.append(" nulls last");
            }
        }
    }

    // Select compilation

    public static SqlCompiled compileSelect(Select select, DbmsSqlSyntax dbmsSyntax, boolean generateQueryMapping, boolean readForeignFields, CompilerDomainModelReader modelReader) {
        return compileSelect(select, dbmsSyntax, generateQueryMapping, readForeignFields, false, modelReader);
    }

    public static SqlCompiled compileSelect(Select select, DbmsSqlSyntax dbmsSyntax, boolean generateQueryMapping, boolean readForeignFields, boolean compileExpressions, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = buildSelect(select, dbmsSyntax, generateQueryMapping, readForeignFields, compileExpressions, null, null, modelReader);
        return sqlBuild.toSqlCompiled();
    }

    // Insert compilation

    public static SqlCompiled compileInsert(Insert insert, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = buildInsert(insert, dbmsSyntax, modelReader);
        return sqlBuild.toSqlCompiled();
    }

    // Update compilation

    public static SqlCompiled compileUpdate(Update update, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = buildUpdate(update, dbmsSyntax, modelReader);
        return sqlBuild.toSqlCompiled();
    }

    // Delete compilation

    public static SqlCompiled compileDelete(Delete delete, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = buildDelete(delete, dbmsSyntax, modelReader);
        return sqlBuild.toSqlCompiled();
    }


     /*** Private entry points ***/

     // Select compilation

    public static SqlBuild buildSelect(Select select, Options parentOptions) {
        Options o = parentOptions;
        return buildSelect(select, o.build.getDbmsSyntax(), o.generateQueryMapping, o.readForeignFields, o.compileExpressions, o.build, o.clause, o.modelReader);
    }

    public static SqlBuild buildSelect(Select select, DbmsSqlSyntax dbmsSyntax, boolean generateQueryMapping, boolean readForeignFields, boolean compileExpressions, SqlBuild parent, SqlClause parentClause, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = createSqlOrderBuild(select, SqlClause.SELECT, dbmsSyntax, parent, modelReader);
        // If this select's primary domain class came from a CTE alias, override the SQL table name
        if (select.getDomainClassCteAlias() != null)
            sqlBuild.setCteTableName(select.getDomainClassCteAlias());
        // Register any additional FROM entities (multiple FROM and CTE support)
        if (select.getAdditionalFromEntities() != null)
            for (Object entity : select.getAdditionalFromEntities()) {
                Object[] entityArr = (Object[]) entity;
                String alias = (String) entityArr[1];
                if (entityArr.length > 2) {
                    // CTE reference: {domainClass, alias, cteName}
                    String cteName = (String) entityArr[2];
                    String realTableName = modelReader.getDomainClassSqlTableName(entityArr[0]);
                    sqlBuild.registerCteFromTable(cteName, realTableName, alias);
                } else {
                    sqlBuild.registerFromTable(modelReader.getDomainClassSqlTableName(entityArr[0]), alias);
                }
            }
        // Register lateral subqueries
        if (select.getLateralSubqueries() != null)
            for (Object lateral : select.getLateralSubqueries()) {
                Object[] lateralArr = (Object[]) lateral;
                String alias = (String) lateralArr[0];
                Select<?> subquery = (Select<?>) lateralArr[1];
                sqlBuild.registerLateralSubquery(alias, subquery, dbmsSyntax, modelReader);
            }
        sqlBuild.setDistinct(select.isDistinct());
        boolean grouped = select.getGroupBy() != null;
        if (select.isIncludeIdColumn() || select.getFields() == null /* <= because a SQL select must have at least 1 column to read */)
            sqlBuild.addColumnInClause(select.isUseRowNumberAsId() ? null : sqlBuild.getTableAlias(), select.isUseRowNumberAsId() ? "row_number() over ()" : modelReader.getDomainClassPrimaryKeySqlColumnName(select.getDomainClass()), null, null, SqlClause.SELECT, "", grouped, false, true);
        if (select.getFields() != null)
            compileExpression(select.getFields(), new Options(sqlBuild, SqlClause.SELECT, ", ", grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader));
        if (select.getGroupBy() != null)
            compileExpression(select.getGroupBy(), new Options(sqlBuild, SqlClause.GROUP_BY, ", ", grouped, false, false, modelReader));
        if (select.getHaving() != null)
            compileExpression(select.getHaving(), new Options(sqlBuild, SqlClause.HAVING, ", ", grouped, false, false, modelReader));
        return buildCommonSqlOrder(select, sqlBuild, grouped, dbmsSyntax, parent, parentClause, modelReader);
    }

    private static SqlBuild createSqlOrderBuild(DqlStatement dqlStatement, SqlClause sqlClause, DbmsSqlSyntax dbmsSyntax, SqlBuild parent, CompilerDomainModelReader modelReader) {
        return new SqlBuild(parent, dqlStatement.getDomainClass(), dqlStatement.getDomainClassAlias(), sqlClause, dbmsSyntax, modelReader);
    }

    private static SqlBuild buildCommonSqlOrder(DqlStatement dqlStatement, SqlBuild sqlBuild, boolean grouped, DbmsSqlSyntax dbmsSyntax, SqlBuild parent, SqlClause parentClause, CompilerDomainModelReader modelReader) {
        if (dqlStatement.getWhere() != null)
            compileExpression(dqlStatement.getWhere(), new Options(sqlBuild, SqlClause.WHERE, null, grouped, false, false, modelReader));
        if (dqlStatement.getOrderBy() != null)
            compileExpression(dqlStatement.getOrderBy(), new Options(sqlBuild, SqlClause.ORDER_BY, ", ", grouped, false, false, modelReader));
        if (dqlStatement.getLimit() != null && dbmsSyntax != HsqlSyntax.get()) // temporary fix
            compileExpression(dqlStatement.getLimit(), new Options(sqlBuild, SqlClause.LIMIT, null, grouped, false, false, modelReader));
        if (dqlStatement instanceof Select<?> select && select.getOffset() != null)
            compileExpression(select.getOffset(), new Options(sqlBuild, SqlClause.OFFSET, null, grouped, false, false, modelReader));
        if (parent != null)
            parent.prepareAppend(parentClause, null).append(sqlBuild.toSql()); // is it the right way?
        dqlStatement.setCacheable(sqlBuild.isCacheable());
        return sqlBuild;
    }

    // Update compilation

    private static SqlBuild buildInsert(Insert insert, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = createSqlOrderBuild(insert, SqlClause.INSERT, dbmsSyntax, null, modelReader);
        String primaryKeySqlColumnName = modelReader.getDomainClassPrimaryKeySqlColumnName(insert.getDomainClass());
        sqlBuild.setAutoGeneratedKeyColumnNames(new String[]{primaryKeySqlColumnName});
        Options insertOptions = new Options(sqlBuild, SqlClause.INSERT, ", ", false, false, false, modelReader);
        Options valuesOptions = new Options(sqlBuild, SqlClause.VALUES, ", ", false, false, false, modelReader);
        for (Expression expression : insert.getSetClause().getExpressions()) {
            Equals equals = (Equals) expression;
            compileExpression(equals.getLeft(), insertOptions);
            compileExpression(equals.getRight(), valuesOptions);
        }
        buildCommonSqlOrder(insert, sqlBuild, false, dbmsSyntax, null, null, modelReader);
        if (dbmsSyntax.hasInsertReturningClause())
            sqlBuild.prepareAppend(new Options(sqlBuild, SqlClause.RETURNING, ", ", false, false, false, modelReader)).append(primaryKeySqlColumnName);
        return sqlBuild;
    }

    // Update compilation

    private static SqlBuild buildUpdate(Update update, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = createSqlOrderBuild(update, SqlClause.UPDATE, dbmsSyntax, null, modelReader);
        compileExpression(update.getSetClause(), new Options(sqlBuild, SqlClause.UPDATE, ", ", false, false, false, modelReader));
        return buildCommonSqlOrder(update, sqlBuild, false, dbmsSyntax, null, null, modelReader);
    }

    // Delete compilation

    private static SqlBuild buildDelete(Delete delete, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader modelReader) {
        SqlBuild sqlBuild = createSqlOrderBuild(delete, SqlClause.DELETE, dbmsSyntax, null, modelReader);
        return buildCommonSqlOrder(delete, sqlBuild, false, dbmsSyntax, null, null, modelReader);
    }

    // Expression compilation

    public static void compileExpression(Expression expression, Options options) {
        Class<? extends Expression> expressionClass = expression.getClass();
        AbstractTermSqlCompiler termSqlCompiler = termCompilers.get(expressionClass);
        if (termSqlCompiler == null) {
            /* J2ME CLDC
            // trying to find the compiler of a super class (ex: DomainField compiler is actually a Term compiler)
            for (Class superClass = expressionClass.getSuperclass(); superClass != null; superClass = superClass.getSuperclass()) {
                termSqlCompiler = termCompilers.get(superClass);
                if (termSqlCompiler != null) {
                    termCompilers.put(expressionClass, termSqlCompiler);
                    break;
                }
            }
            if (termSqlCompiler == null) */
            throw new IllegalArgumentException("Sql not compilable: " + expression);
        }
        termSqlCompiler.compileExpressionToSql(expression, options);
    }

    /*** Helper methods ***/

    public static String toSqlString(String name) {
        if (name == null || name.length() < 2)
            return name;
        StringBuilder sb = new StringBuilder();
        boolean underscoreAllowed = false;
        int i0 = 0, i = 1;
        for (; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (underscoreAllowed)
                    sb.append('_');
                for (int j = i0; j < i; j++) {
                    c = name.charAt(j);
                    sb.append(Character.toLowerCase(c));
                }
                underscoreAllowed = c != '_' && i > i0 + 1; // no underscore after an underscore (ex: current_date) or between two consecutive uppercases
                i0 = i;
            }
        }
        if (underscoreAllowed)
            sb.append('_');
        for (int j = i0; j < i; j++)
            sb.append(Character.toLowerCase(name.charAt(j)));
        return sb.toString();
    }
}
