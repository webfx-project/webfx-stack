package dev.webfx.stack.framework.shared.interceptors.dqlquery;

import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.framework.shared.services.datasourcemodel.DataSourceModelService;
import dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.platform.shared.services.datasource.LocalDataSourceService;
import dev.webfx.stack.platform.shared.services.query.QueryArgument;
import dev.webfx.stack.platform.shared.services.query.QueryResult;
import dev.webfx.stack.platform.shared.services.query.spi.QueryServiceProvider;
import dev.webfx.stack.platform.async.Future;
import dev.webfx.platform.shared.util.serviceloader.SingleServiceProvider;

/**
 * @author Bruno Salmon
 */
public class DqlQueryInterceptorModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-framework-shared-orm-dql-query-interceptor";
    }

    @Override
    public int getBootLevel() {
        return APPLICATION_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // The purpose of this interceptor is to automatically translate DQL to SQL when the query reaches its local data source
        SingleServiceProvider.registerServiceInterceptor(QueryServiceProvider.class, targetProvider ->
                argument -> interceptAndExecuteQuery(argument, targetProvider)
        );
    }

    private Future<QueryResult> interceptAndExecuteQuery(QueryArgument argument, QueryServiceProvider targetProvider) {
        String language = argument.getLanguage();
        Object dataSourceId = argument.getDataSourceId();
        if (language != null && LocalDataSourceService.isDataSourceLocal(dataSourceId)) {
            DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
            if (dataSourceModel != null) {
                // Translating DQL to SQL
                String statement = argument.getStatement(); // can be DQL or SQL
                String sqlStatement = dataSourceModel.translateQuery(language, statement);
                if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                    //Logger.log("Translated to: " + sqlStatement);
                    argument = QueryArgument.builder().copy(argument).setLanguage(null).setStatement(sqlStatement).build();
                }
            }
        }
        return targetProvider.executeQuery(argument);
    }
}
