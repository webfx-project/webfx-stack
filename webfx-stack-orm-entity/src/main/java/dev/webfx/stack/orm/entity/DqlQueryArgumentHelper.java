package dev.webfx.stack.orm.entity;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.platform.util.Arrays;

/**
 * @author Bruno Salmon
 */
public final class DqlQueryArgumentHelper {

    private final static String DQL_LANGUAGE = "DQL";
    private final static String QUERY_LANGUAGE = DQL_LANGUAGE; // can be set to "DQL", "SQL" or null (null = default = SQL)

    public static QueryArgument createQueryArgument(String dqlQuery, Object[] parameters, DataSourceModel dataSourceModel, AggregateScope aggregateScope) {
        return QueryArgument.builder()
                .setLanguage(DQL_LANGUAGE)
                .setStatement(DQL_LANGUAGE.equals(QUERY_LANGUAGE) ? dqlQuery : dataSourceModel.translateQuery(DQL_LANGUAGE, dqlQuery))
                .setParameters(resolveParameters(parameters))
                .setDataSourceId(dataSourceModel.getDataSourceId())
                .addDataScope(aggregateScope)
                .build();
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
