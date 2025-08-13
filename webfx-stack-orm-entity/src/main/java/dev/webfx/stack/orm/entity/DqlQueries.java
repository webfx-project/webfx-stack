package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryArgumentBuilder;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;

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
        return newQueryArgument(
            QueryArgument.builder()
                .setStatement(dqlQuery)
                .setParameters(parameters)
                .setDataSourceId(dataSourceId)
                .addDataScope(aggregateScope)
        );
    }

    private static QueryArgument newQueryArgument(QueryArgumentBuilder builder) {
        return builder
            .setLanguage(DQL_LANGUAGE)
            .setStatement(translateQuery(builder.getStatement(), builder.getDataSourceId()))
            .setParameters(resolveParameters(builder.getParameters()))
            .build();
    }

    private static String translateQuery(String dqlQuery, Object dataSourceId) {
        if (DQL_LANGUAGE.equals(QUERY_LANGUAGE))
            return dqlQuery;
        DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
        return dataSourceModel.translateQuery(DQL_LANGUAGE, dqlQuery);
    }

    private static Object[] resolveParameters(Object[] parameters) {
        if (parameters != null) {
            boolean hasResolved = false;
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                Object primaryKey = Entities.getPrimaryKey(parameter);
                if (primaryKey != parameter) {
                    if (!hasResolved) {
                        parameters = Arrays.clone(parameters, Object[]::new);
                        hasResolved = true;
                    }
                    parameters[i] = primaryKey;
                }
            }
        }
        return parameters;
    }
}
