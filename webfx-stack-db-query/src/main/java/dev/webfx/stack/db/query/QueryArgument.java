package dev.webfx.stack.db.query;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.db.datascope.DataScope;

import java.util.Objects;

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

        if (!Objects.equals(dataSourceId, that.dataSourceId)) return false;
        if (!Objects.equals(language, that.language)) return false;
        if (!Objects.equals(statement, that.statement)) return false;

        // We need to be quite flexible with the parameters, especially with the numbers, as they might be of different
        // types after (de)serialisation (ex: a 5 Integer can become a 5 Short, but they should still be considered as
        // equals). So we will use Numbers.sameValue() for the check.

        // Note: it's ok to consider parameter Object[0] and null to be equals
        int n1 = parameters == null ? 0 : parameters.length;
        int n2 = that.parameters == null ? 0 : that.parameters.length;
        if (n1 != n2) return false;
        for (int i = 0; i < n1; i++) {
            Object p1 = parameters[i], p2 = that.parameters[i];
            if (!Objects.equals(p1, p2)) { // Not equals, but might be 2 numbers of identical value
                if (!(p1 instanceof Number && p2 instanceof Number && Numbers.identicalValues((Number) p1, (Number) p2)))
                    return false; // if not, they are definitely not equals
            }
        }
        return true;
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
