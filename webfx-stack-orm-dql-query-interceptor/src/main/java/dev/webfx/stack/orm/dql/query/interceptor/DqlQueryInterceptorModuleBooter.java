package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

/**
 * @author Bruno Salmon
 */
public class DqlQueryInterceptorModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-orm-dql-query-interceptor";
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
