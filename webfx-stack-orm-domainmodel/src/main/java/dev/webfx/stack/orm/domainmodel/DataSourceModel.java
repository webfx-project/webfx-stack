package dev.webfx.stack.orm.domainmodel;

import dev.webfx.stack.orm.domainmodel.lciimpl.CompilerDomainModelReaderImpl;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.DbmsSqlSyntax;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.Union;
import dev.webfx.stack.orm.expression.terms.WithSelect;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class DataSourceModel implements HasDomainModel {

    private final Object dataSourceId;
    private final DbmsSqlSyntax dbmsSqlSyntax;
    private final DomainModel domainModel;
    private CompilerDomainModelReader compilerDomainModelReader;
    // Bounded, and per data scope — see ScopedCompilationCache for why in both directions, and why neither
    // weak nor soft references are the answer. (Was a plain HashMap keyed on client-chosen text, unbounded.)
    private final ScopedCompilationCache sqlCompiledCache = new ScopedCompilationCache();
    private final ScopedCompilationCache sqlCompiledWithExpressionsCache = new ScopedCompilationCache();

    public DataSourceModel(Object dataSourceId, DbmsSqlSyntax dbmsSqlSyntax, DomainModel domainModel) {
        this.dataSourceId = dataSourceId;
        this.dbmsSqlSyntax = dbmsSqlSyntax;
        this.domainModel = domainModel;
    }

    public Object getDataSourceId() {
        return dataSourceId;
    }

    public DbmsSqlSyntax getDbmsSqlSyntax() {
        return dbmsSqlSyntax;
    }

    @Override
    public DomainModel getDomainModel() {
        return domainModel;
    }

    public CompilerDomainModelReader getCompilerDomainModelReader() {
        if (compilerDomainModelReader == null)
            compilerDomainModelReader = new CompilerDomainModelReaderImpl(getDomainModel());
        return compilerDomainModelReader;
    }

    public SqlCompiled parseAndCompileSelect(String stringSelect) {
        SqlCompiled sqlCompiled = sqlCompiledCache.get(stringSelect);
        //if (sqlCompiled != null) Logger.log("Reusing cached sql compiled! :-)");
        if (sqlCompiled == null)
            sqlCompiledCache.put(stringSelect, sqlCompiled = compileSelectOrWithSelect(parseStatement(stringSelect), false));
        return sqlCompiled;
    }

    public SqlCompiled parseAndCompileSelect(String stringSelect, boolean compileExpressions) {
        return parseAndCompileSelect(stringSelect, compileExpressions, null);
    }

    /**
     * Compiles under a data scope, and caches per scope.
     *
     * <p><b>The token is part of the key, and that is not an optimisation.</b> This cache is keyed by the
     * statement's TEXT, and two callers send identical text — so with scope injected and the key unchanged,
     * the first caller's scope would be baked into the cached SQL and served to the second. That is the same
     * trap as compiling a caller's identity to a literal, one level up, and it fails the same way: silently,
     * and in the direction that hands one member's rows to another.
     *
     * <p>It stays bounded because a token names one of a fixed set of scopes, not one caller — which is the
     * argument for named scopes over predicates assembled per request, showing up here as the reason the
     * cache can exist at all.
     */
    public SqlCompiled parseAndCompileSelect(String stringSelect, boolean compileExpressions, Object scopeToken) {
        ScopedCompilationCache cache = compileExpressions ? sqlCompiledWithExpressionsCache : sqlCompiledCache;
        cache.noteScopeToken(scopeToken);
        String key = ScopedCompilationCache.keyOf(stringSelect, scopeToken);
        SqlCompiled sqlCompiled = cache.get(key);
        if (sqlCompiled == null)
            cache.put(key, sqlCompiled = ExpressionSqlCompiler.compileForScope(parseStatement(stringSelect), scopeToken,
                    getDbmsSqlSyntax(), true, true, compileExpressions, getCompilerDomainModelReader()));
        return sqlCompiled;
    }

    private SqlCompiled compileSelectOrWithSelect(DqlStatement<?> statement, boolean compileExpressions) {
        if (statement instanceof WithSelect)
            return ExpressionSqlCompiler.compileWithSelect((WithSelect<?>) statement, getDbmsSqlSyntax(), true, true, compileExpressions, getCompilerDomainModelReader());
        if (statement instanceof Union)
            return ExpressionSqlCompiler.compileUnion((Union<?>) statement, getDbmsSqlSyntax(), true, true, compileExpressions, getCompilerDomainModelReader());
        return compileSelect((Select<?>) statement, compileExpressions);
    }

    public <T> Select<T> parseSelect(String definition) {
        return getDomainModel().parseSelect(definition);
    }


    public SqlCompiled compileSelect(Select<?> select) {
        return compileSelect(select, false);
    }

    public SqlCompiled compileSelect(Select<?> select, boolean compileExpressions) {
        return ExpressionSqlCompiler.compileSelect(select, getDbmsSqlSyntax(), true, true, compileExpressions, getCompilerDomainModelReader());
    }

    public String translateQuery(String queryLanguage, String query) {
        if ("DQL".equalsIgnoreCase(queryLanguage))
            return parseAndCompileSelect(query).getSql();
        return query;
    }

    public SqlCompiled parseAndCompileStatement(String statement) {
        SqlCompiled sqlCompiled = sqlCompiledCache.get(statement);
        //if (sqlCompiled != null) Logger.log("Reusing cached sql compiled! :-)");
        if (sqlCompiled == null)
            sqlCompiledCache.put(statement, sqlCompiled = compileStatement(parseStatement(statement)));
        return sqlCompiled;
    }

    public <T> DqlStatement<T> parseStatement(String definition) {
        return getDomainModel().parseStatement(definition);
    }

    public SqlCompiled compileStatement(DqlStatement<?> statement) {
        return ExpressionSqlCompiler.compileStatement(statement, getDbmsSqlSyntax(), getCompilerDomainModelReader());
    }

    public String translateStatementIfDql(String statementLang, String statementString) {
        if ("DQL".equalsIgnoreCase(statementLang))
            return parseAndCompileStatement(statementString).getSql();
        return statementString;
    }
}
