package dev.webfx.stack.db.submit;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.db.datascope.DataScope;

/**
 * @author Bruno Salmon
 */
public final class SubmitArgument {

    private final transient SubmitArgument originalArgument;
    private final Object dataSourceId;
    private final DataScope dataScope;
    private final boolean returnGeneratedKeys;
    private final String language;
    private final String statement;
    private final Object[] parameters;

    public SubmitArgument(SubmitArgument originalArgument, Object dataSourceId, DataScope dataScope, boolean returnGeneratedKeys, String language, String statement, Object[] parameters) {
        this.originalArgument = originalArgument;
        this.dataSourceId = dataSourceId;
        this.dataScope = dataScope;
        this.returnGeneratedKeys = returnGeneratedKeys;
        this.language = language;
        this.statement = statement;
        this.parameters = parameters;
    }

    public SubmitArgument getOriginalArgument() {
        return originalArgument;
    }

    public Object getDataSourceId() {
        return dataSourceId;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public boolean returnGeneratedKeys() {
        return returnGeneratedKeys;
    }

    public String getLanguage() {
        return language;
    }

    public String getStatement() {
        return statement;
    }

    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "SubmitArgument('" + statement + (parameters == null ? "'" : "', " + Arrays.toString(parameters)) + ')';
    }

    public static SubmitArgumentBuilder builder() {
        return new SubmitArgumentBuilder();
    }

}
