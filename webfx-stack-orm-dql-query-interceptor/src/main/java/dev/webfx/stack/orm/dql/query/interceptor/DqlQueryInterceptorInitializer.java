package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.ClientQueryGuard;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.ServerCallerParameters;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.dql.sqlcompiler.mapping.QueryRowToEntityMapping;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.expression.terms.function.ServerParameterFunction;

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
        // And the check a CLIENT's query passes before any of that: registered here because this is the module that
        // knows how to compile DQL, which the client-query endpoints must not depend on. Inert where nothing has
        // been declared secret — including in the browser, where this initializer also runs.
        ClientQueryGuard.registerInspector(new DqlClientQueryInspector());
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
                    boolean compileExpressions = !argument.isHasDqlRuntime();
                    // Compiled under the caller's data scope, which the endpoint stamped on the argument.
                    // Null for every server-origin read — an endpoint's own lookup never passed the endpoint
                    // that sets it — so those compile exactly as they always have.
                    SqlCompiled sqlCompiled = dataSourceModel.parseAndCompileSelect(statement, compileExpressions, argument.getScopeToken()); // May raise an exception on syntax error or unknown fields
                    String sqlStatement = sqlCompiled.getSql();
                    if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                        //Console.log("[DQL] DQL: " + statement + "\nSQL: " + sqlStatement + "\n[DQL] Mapping: " + sqlCompiled.getQueryMapping());
                        QueryRowToEntityMapping queryMapping = sqlCompiled.getQueryMapping();
                        QueryArgument dqlArgument = QueryArgument.builder().copy(argument)
                            .setLanguage(null)
                            .setStatement(sqlStatement)
                            .setParameters(reorderNamedParameters(sqlCompiled, argument))
                            .build();
                        return targetProvider.executeQuery(dqlArgument)
                            .map(result -> {
                                // Always attach the entity mapping to the in-memory QueryResult — the wire-level
                                // "ship it or strip it?" decision happens downstream (ExecuteQueryMethodEndpoint
                                // for one-shot calls, ServerQueryPushServiceProviderBase for push subscriptions),
                                // which already strip the mapping when the client claims a cache hit.
                                //
                                // Previously this was gated on `argument.isSendMetadata()`, which silently dropped
                                // the mapping whenever a client subscribed with `sendMetadata=false` (warm cache).
                                // That poisoned the server-side `queryInfo.lastQueryResult` cache: every later
                                // subscriber to the same query inherited the missing mapping, so push streams
                                // decoded rows as positional `col0`/`col1` for their whole lifetime — and any
                                // restart of the client's per-statement cache (StrictMode remount, BusProvider
                                // teardown, etc.) left the activity feed stuck empty until a server restart.
                                if (result != null && queryMapping != null)
                                    result.setEntityMapping(queryMapping);
                                return result;
                            });
                    }
                } catch (ServerCallerParameters.CallerValueUnavailableException e) {
                    // NOT a translation failure, and its message has to reach the client as it is: it says
                    // the read can be retried. The wrapping below would bury that inside a DQL error and
                    // echo the statement, for a refusal that has nothing to do with the statement.
                    return Future.failedFuture(e.getMessage());
                } catch (Exception e) {
                    // This message is BOTH logged and returned to the caller (failedFuture below), so
                    // the bind values must not be in it: a count, never the contents. The statement and
                    // the parameter NAMES stay - they are what a translation failure is diagnosed from,
                    // and they are metadata rather than anyone's data.
                    Exception ex = new IllegalArgumentException("Error while translating DQL query to SQL: " + e.getMessage() + "\nDQL query:\n" + statement + "\nParameters: " + QueryArgument.describeParameters(argument.getParameters())+ "\nParameter names: " + Arrays.toString(argument.getParameterNames()));
                    Console.error(ex);
                    return Future.failedFuture(ex);
                }
            }
        }
        return targetProvider.executeQuery(argument);
    }

    /**
     * The values to bind, in the order the compiled SQL numbers them: the caller's own positional block
     * first, then the named parameters in the order the compiler allocated them.
     *
     * <p><b>Names may cover only the TRAILING values.</b> A caller has always either named all of its values
     * or none of them, and those two stay exactly as they were. The third shape is what a server-added term
     * produces — the caller's array with a value appended and a name for that value alone — and it is the one
     * this exists to bind correctly, since the compiler now numbers such a name above the caller's $n rather
     * than on top of them.
     *
     * <p>Static and package-visible so it can be checked without an interceptor; it reads no instance state.
     */
    static Object[] reorderNamedParameters(SqlCompiled sqlCompiled, QueryArgument argument) {
        List<String> expectedParameterNames = sqlCompiled.getParameterNames();
        int namedCount = Collections.size(expectedParameterNames);
        Object[] parameters = argument.getParameters();
        if (namedCount == 0) {
            // The statement names nothing, so anything named that was supplied is surplus and is dropped
            // rather than bound. This is how a caller term attached on a text match survives a statement that
            // merely mentioned CALLER_ in a literal: the endpoint cannot parse, so it may over-attach, and
            // over-attaching has to cost nothing.
            int suppliedNames = Arrays.length(argument.getParameterNames());
            if (suppliedNames > 0 && Arrays.length(parameters) >= suppliedNames) {
                Object[] positionalOnly = new Object[Arrays.length(parameters) - suppliedNames];
                for (int i = 0; i < positionalOnly.length; i++)
                    positionalOnly[i] = parameters[i];
                return positionalOnly;
            }
            return parameters; // the statement has no named parameter: the values are the positional ones
        }
        String[] parameterNames = argument.getParameterNames();
        // A server-supplied term is the server's to value, so its absence is a server fault and has to say so
        // here. Left to the early return below it reaches the driver as a bind-count mismatch, which names
        // neither the statement nor the term that was never resolved.
        for (int i = 0; i < namedCount; i++) {
            String name = expectedParameterNames.get(i);
            if (name.startsWith(ServerParameterFunction.PARAMETER_NAME_PREFIX)
                    && Arrays.indexOf(parameterNames, name) < 0)
                throw new IllegalArgumentException("The statement uses the server-supplied term '" + name
                        + "' and nothing resolved it. Its value is not the caller's to send.");
        }
        if (Arrays.isEmpty(parameterNames)) // Happens with search parameters (their values don't have names)
            return parameters; // We assume they are in the correct order
        int suppliedCount = Arrays.length(parameters);
        int suppliedNamedCount = Arrays.length(parameterNames);
        if (suppliedNamedCount > suppliedCount)
            throw new IllegalArgumentException("More parameter names (" + suppliedNamedCount + ") than parameters (" + suppliedCount + ")");
        // Anything ahead of the named block is a positional value, and stays where the caller put it.
        int positionalCount = suppliedCount - suppliedNamedCount;
        if (positionalCount != sqlCompiled.getPositionalParameterCount())
            throw new IllegalArgumentException("The statement binds " + sqlCompiled.getPositionalParameterCount()
                    + " positional parameters but " + positionalCount + " were supplied ahead of the named ones");
        Object[] orderedParameters = new Object[positionalCount + namedCount];
        for (int i = 0; i < positionalCount; i++)
            orderedParameters[i] = parameters[i];
        for (int i = 0; i < namedCount; i++) {
            String name = expectedParameterNames.get(i);
            int index = Arrays.indexOf(parameterNames, name);
            if (index < 0)
                throw new IllegalArgumentException("Expected parameter '" + name + "' not found in the passed parameters");
            Object value = parameters[positionalCount + index];
            // A value the server could not supply must not be BOUND: binding it as null is the empty result
            // that cannot be told from "you may read nothing", and binding the marker itself would hand the
            // driver an object it cannot encode. ServerCallerParameters refuses such a query at the endpoint,
            // so nothing should arrive here — this is the floor under that, at the one place that knows for
            // certain the statement binds this name, and it is also what would refuse one that reached a
            // re-fired subscription.
            if (value == ServerCallerParameters.UNAVAILABLE)
                throw new ServerCallerParameters.CallerValueUnavailableException(
                    ServerCallerParameters.CALLER_VALUE_UNAVAILABLE_REFUSED);
            orderedParameters[positionalCount + i] = value;
        }
        return orderedParameters;
    }
}
