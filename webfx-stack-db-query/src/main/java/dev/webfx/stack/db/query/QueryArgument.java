package dev.webfx.stack.db.query;

import dev.webfx.stack.db.datascope.DataScope;

/**
 * @author Bruno Salmon
 */
public final class QueryArgument {

    private final transient QueryArgument originalArgument;
    private final Object dataSourceId;
    private final DataScope dataScope;
    private final String language;
    private final String statement;
    private final Object[] parameters;

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object... parameters) {
        this.originalArgument = originalArgument;
        this.dataSourceId = dataSourceId;
        this.dataScope = dataScope;
        this.language = language;
        this.statement = statement;
        this.parameters = parameters;
    }

    public QueryArgument getOriginalArgument() {
        return originalArgument;
    }

    public Object getDataSourceId() {
        return dataSourceId;
    }

    public DataScope getDataScope() {
        return dataScope;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryArgument that = (QueryArgument) o;

        if (!dataSourceId.equals(that.dataSourceId)) return false;
        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        if (statement != null ? !statement.equals(that.statement) : that.statement != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return java.util.Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = dataSourceId.hashCode();
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (statement != null ? statement.hashCode() : 0);
        result = 31 * result + java.util.Arrays.hashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        return "QueryArgument{" +
                "dataSourceId=" + dataSourceId +
                ", queryLang='" + language + '\'' +
                ", queryString='" + statement + '\'' +
                ", parameters=" + java.util.Arrays.toString(parameters) +
                '}';
    }

    public static QueryArgumentBuilder builder() {
        return new QueryArgumentBuilder();
    }

}
