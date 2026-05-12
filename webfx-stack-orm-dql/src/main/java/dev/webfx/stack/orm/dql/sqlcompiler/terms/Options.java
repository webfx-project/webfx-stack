package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlBuild;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlClause;
import dev.webfx.platform.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class Options {
    public final SqlBuild build;
    public final SqlClause clause;
    public final String separator;
    public final boolean grouped;
    public final boolean generateQueryMapping;
    public final boolean readForeignFields;
    public final boolean compileExpressions;
    public final CompilerDomainModelReader modelReader;

    public Options(SqlBuild build, SqlClause clause, String separator, boolean grouped, boolean generateQueryMapping, boolean readForeignFields, CompilerDomainModelReader modelReader) {
        this(build, clause, separator, grouped, generateQueryMapping, readForeignFields, false, modelReader);
    }

    public Options(SqlBuild build, SqlClause clause, String separator, boolean grouped, boolean generateQueryMapping, boolean readForeignFields, boolean compileExpressions, CompilerDomainModelReader modelReader) {
        this.separator = separator;
        this.build = build;
        this.clause = clause;
        this.grouped = grouped;
        this.generateQueryMapping = generateQueryMapping;
        this.readForeignFields = readForeignFields;
        this.compileExpressions = compileExpressions;
        this.modelReader = modelReader;
    }

    public boolean isTopLevelSelect() {
        return clause == SqlClause.SELECT && !build.hasParent();
    }

    public Options changeSeparator(String separator) {
        if (Objects.areEquals(this.separator, separator))
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }

    public Options changeReadForeignFields(boolean readForeignFields) {
        if (this.readForeignFields == readForeignFields)
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }

    public Options changeGenerateQueryMapping(boolean generateQueryMapping) {
        if (this.generateQueryMapping == generateQueryMapping)
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }

    public Options changeSeparatorGenerateQueryMapping(String separator, boolean generateQueryMapping) {
        if (Objects.areEquals(this.separator, separator) && this.generateQueryMapping == generateQueryMapping)
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }

    public Options changeSeparatorGroupedGenerateQueryMapping(String separator, boolean grouped, boolean generateQueryMapping) {
        if (Objects.areEquals(this.separator, separator) && this.grouped == grouped && this.generateQueryMapping == generateQueryMapping)
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }

    public Options changeCompileExpressions(boolean compileExpressions) {
        if (this.compileExpressions == compileExpressions)
            return this;
        return new Options(build, clause, separator, grouped, generateQueryMapping, readForeignFields, compileExpressions, modelReader);
    }
}
