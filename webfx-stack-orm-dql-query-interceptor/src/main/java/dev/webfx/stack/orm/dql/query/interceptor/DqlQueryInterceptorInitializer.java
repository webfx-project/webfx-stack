package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;

/**
 * @author Bruno Salmon
 */
public class DqlQueryInterceptorInitializer implements ApplicationJob {

    @Override
    public void onInit() {
        // The purpose of this interceptor is to automatically translate DQL to SQL when the query reaches its local data source
        SingleServiceProvider.registerServiceInterceptor(QueryServiceProvider.class, targetProvider ->
                argument -> interceptAndExecuteQuery(argument, targetProvider)
        );
    }

    private Future<QueryResult> interceptAndExecuteQuery(QueryArgument argument, QueryServiceProvider targetProvider) {
        // The language must be specified if a translation is required
        String language = argument.getLanguage();
        if (language != null) {
            // Also we translate only if the datasource is local (when we reached the final endpoint), so we ask the
            // LocalDataSourceService for this. However, on server start, LocalDataSourceService might not yet be
            // initialised. Modality for example needs to first read the local database connection details from
            // configuration files to initialise, which sometimes is not immediate. In that case, we wait it is ready.
            if (!LocalDataSourceService.isInitialised()) { // can happen on server start
                Promise<QueryResult> promise = Promise.promise();
                LocalDataSourceService.onInitialised(() -> // when it's ready, we can continue
                        interceptAndExecuteQuery(argument, targetProvider).onComplete(promise));
                return promise.future();
            }
            // Now the LocalDataSourceService is initialised, and we can ask it if it's a local database
            Object dataSourceId = argument.getDataSourceId();
            if (LocalDataSourceService.isDataSourceLocal(dataSourceId)) {
                String statement = argument.getStatement(); // can be DQL or SQL
                DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
                // Translating DQL to SQL
                try {
                    String sqlStatement = dataSourceModel.translateQuery(language, statement); // May raise an exception on syntax error or unknown fields
                    if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                        //Console.log("Translated to: " + sqlStatement);
                        QueryArgument dqlArgument = QueryArgument.builder().copy(argument).setLanguage(null).setStatement(sqlStatement).build();
                        return targetProvider.executeQuery(dqlArgument);
                    }
                } catch (Exception e) {
                    Exception ex = new IllegalArgumentException("Error while translating query '" + statement + "' to " + language, e);
                    Console.log(ex);
                    return Future.failedFuture(ex);
                }
            }
        }
        return targetProvider.executeQuery(argument);
    }
}
