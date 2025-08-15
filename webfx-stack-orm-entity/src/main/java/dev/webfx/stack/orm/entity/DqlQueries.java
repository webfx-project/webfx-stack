package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class DqlQueries {

    private final static String DQL_LANGUAGE = "DQL";
    private final static String QUERY_LANGUAGE = DQL_LANGUAGE; // can be set to "DQL", "SQL" or null (null = default = SQL)

    public static QueryArgument newQueryArgumentForDefaultDataSource(String dqlQuery, Object... parameters) { // the method name must be different from queryArgument() to avoid signature ambiguity
        return newQueryArgument(DataSourceModelService.getDefaultDataSourceId(), dqlQuery, parameters);
    }

    public static QueryArgument newQueryArgument(Object dataSourceId, String dqlQuery, Object... parameters) {
        return newQueryArgument(dataSourceId, null, dqlQuery, parameters);
    }

    public static QueryArgument newQueryArgument(Object dataSourceId, AggregateScope aggregateScope, String dqlQuery, Object... parameters) {
        String[][] parameterNamesHolder = { null };
        parameters = resolveParameters(parameters, parameterNames -> parameterNamesHolder[0] = parameterNames);
        return newQueryArgument(dataSourceId, aggregateScope, dqlQuery, parameters, parameterNamesHolder[0]);
    }

    public static QueryArgument newQueryArgument(Object dataSourceId, AggregateScope aggregateScope, String dqlQuery, Object[] parameters, String[] parameterNames) {
        return QueryArgument.builder()
            .setDataSourceId(dataSourceId)
            .addDataScope(aggregateScope)
            .setLanguage(DQL_LANGUAGE)
            .setStatement(translateQuery(dqlQuery, dataSourceId))
            .setParameters(parameters)
            .setParameterNames(parameterNames)
            .build();
    }

    private static String translateQuery(String dqlQuery, Object dataSourceId) {
        if (DQL_LANGUAGE.equals(QUERY_LANGUAGE))
            return dqlQuery;
        DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
        return dataSourceModel.translateQuery(DQL_LANGUAGE, dqlQuery);
    }

    public static Object[] resolveParameters(Object[] parameters, Consumer<String[]> parameterNamesSetter) {
        Object[] resolvedParameters = parameters;
        int length = Arrays.length(parameters);
        if (length > 0) {
            if (length == 1 && parameters[0] instanceof NamedParameters namedParameters) {
                resolvedParameters = parameters = namedParameters.get();
                length = Arrays.length(parameters);
            }
            String[] parameterNames = null;
            for (int i = 0; i < length; i++) {
                Object parameter = parameters[i];
                if (parameter instanceof NamedParameter namedParameter) {
                    if (parameterNames == null) {
                        parameterNames = new String[length];
                        resolvedParameters = Arrays.clone(parameters, Object[]::new);
                    }
                    parameterNames[i] = namedParameter.name();
                    resolvedParameters[i] = parameter = namedParameter.value();
                }
                Object primaryKey = Entities.getPrimaryKey(parameter);
                if (primaryKey != parameter) {
                    if (resolvedParameters == parameters)
                        parameters = Arrays.clone(parameters, Object[]::new);
                    resolvedParameters[i] = primaryKey;
                }
            }
            if (parameterNames != null)
                parameterNamesSetter.accept(parameterNames);
        }
        return resolvedParameters;
    }
}
