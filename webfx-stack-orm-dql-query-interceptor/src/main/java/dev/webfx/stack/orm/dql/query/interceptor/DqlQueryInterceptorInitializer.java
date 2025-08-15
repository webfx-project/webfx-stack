package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;

import java.util.List;

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
            // Also, we translate only if the datasource is local (when we reached the final endpoint), so we ask the
            // LocalDataSourceService for this. However, on server start, LocalDataSourceService might not yet be
            // initialized. Modality, for example, needs to first read the local database connection details from
            // configuration files to initialize, which sometimes is not immediate. In that case, we wait until it's ready.
            if (!LocalDataSourceService.isInitialised()) { // can happen on server start
                Promise<QueryResult> promise = Promise.promise();
                LocalDataSourceService.onInitialised(() -> // when it's ready, we can continue
                        interceptAndExecuteQuery(argument, targetProvider).onComplete(promise));
                return promise.future();
            }
            // Now the LocalDataSourceService is initialized, and we can ask it if it's a local database
            Object dataSourceId = argument.getDataSourceId();
            if (LocalDataSourceService.isDataSourceLocal(dataSourceId)) {
                String statement = argument.getStatement(); // can be DQL or SQL
                DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
                // Translating DQL to SQL
                try {
                    String sqlStatement = dataSourceModel.translateQuery(language, statement); // May raise an exception on syntax error or unknown fields
                    if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                        //Console.log("Translated to: " + sqlStatement);
                        QueryArgument dqlArgument = QueryArgument.builder().copy(argument)
                            .setLanguage(null)
                            .setStatement(sqlStatement)
                            .setParameters(reorderNamedParameters(argument, dataSourceModel))
                            .build();
                        return targetProvider.executeQuery(dqlArgument);
                    }
                } catch (Exception e) {
                    Exception ex = new IllegalArgumentException("Error while translating DQL query to SQL: " + e.getMessage() + "\nDQL query:\n" + statement + "\nParameters: " + Arrays.toString(argument.getParameters())+ "\nParameter names: " + Arrays.toString(argument.getParameterNames()));
                    Console.log(ex);
                    return Future.failedFuture(ex);
                }
            }
        }
        return targetProvider.executeQuery(argument);
    }

    private Object[] reorderNamedParameters(QueryArgument argument, DataSourceModel dataSourceModel) {
        SqlCompiled sqlCompiled = dataSourceModel.parseAndCompileSelect(argument.getStatement()); // should be immediate from the cache
        List<String> expectedParameterNames = sqlCompiled.getParameterNames();
        int length = Collections.size(expectedParameterNames);
        Object[] parameters = argument.getParameters();
        if (length == 0)
            return parameters;
        String[] parameterNames = argument.getParameterNames();
        if (Arrays.length(parameterNames) != Arrays.length(parameters))
            throw new IllegalArgumentException("The number of parameter names (" + Arrays.length(parameterNames) + ") does not match the number of parameters (" + Arrays.length(parameters) + ")");
        Object[] orderedParameters = new Object[length];
        for (int i = 0; i < length; i++) {
            String name = expectedParameterNames.get(i);
            int index = Arrays.indexOf(parameterNames, name);
            if (index < 0)
                throw new IllegalArgumentException("Expected parameter '" + name + "' not found in the passed parameters");
            Object value = parameters[index];
            orderedParameters[i] = value;
        }
        return orderedParameters;
    }
}
