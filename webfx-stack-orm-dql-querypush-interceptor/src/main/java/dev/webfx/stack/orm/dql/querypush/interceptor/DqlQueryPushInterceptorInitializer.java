package dev.webfx.stack.orm.dql.querypush.interceptor;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.datascope.aggregate.AggregateScopeBuilder;
import dev.webfx.stack.db.datascope.schema.SchemaScope;
import dev.webfx.stack.db.datascope.schema.SchemaScopeBuilder;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.querypush.DatabaseHealthMonitorInfo;
import dev.webfx.stack.db.querypush.PulseArgument;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.db.querypush.QueryPushMonitorInfo;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;
import dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Select;

/**
 * @author Bruno Salmon
 */
public class DqlQueryPushInterceptorInitializer implements ApplicationJob {

    @Override
    public void onInit() {
        // The purpose of this interceptor is to automatically set the query schema scope if not set (works only with
        // DQL select)
        SingleServiceProvider.registerServiceInterceptor(QueryPushServiceProvider.class, targetProvider ->
                new QueryPushServiceProvider() {
                    @Override
                    public Future<Object> executeQueryPush(QueryPushArgument argument) {
                        return interceptAndExecuteQueryPush(argument, targetProvider);
                    }

                    @Override
                    public void executePulse(PulseArgument argument) {
                        targetProvider.executePulse(argument);
                    }

                    @Override
                    public QueryPushMonitorInfo getMonitorInfo() {
                        // Delegate the monitoring snapshot to the wrapped provider (this
                        // interceptor only rewrites the query scope on executeQueryPush; without
                        // this delegation the call would fall through to the SPI default = null).
                        return targetProvider.getMonitorInfo();
                    }

                    @Override
                    public Future<DatabaseHealthMonitorInfo> getDatabaseHealthInfo() {
                        // Delegate the DB health snapshot too — this interceptor only touches executeQueryPush.
                        return targetProvider.getDatabaseHealthInfo();
                    }

                    @Override
                    public Boolean cancelSqlQuery(long monitorId) {
                        // Delegate cancellation too — this interceptor only touches executeQueryPush.
                        return targetProvider.cancelSqlQuery(monitorId);
                    }

                    @Override
                    public Boolean armSqlAnalyze(String statement) {
                        return targetProvider.armSqlAnalyze(statement);
                    }

                    @Override
                    public SqlAnalyzeResultInfo getSqlAnalyzeResult(String statement) {
                        return targetProvider.getSqlAnalyzeResult(statement);
                    }

                    @Override
                    public Boolean resetSqlAnalyze(String statement) {
                        return targetProvider.resetSqlAnalyze(statement);
                    }

                    @Override
                    public Boolean resetSqlMonitor() {
                        return targetProvider.resetSqlMonitor();
                    }
                }
        );
    }

    private Future<Object> interceptAndExecuteQueryPush(QueryPushArgument argument, QueryPushServiceProvider targetProvider) {
        QueryArgument queryArgument = argument.getQueryArgument();
        if (queryArgument != null && LocalDataSourceService.isDataSourceLocal(argument.getDataSourceId())) {
            String dqlStatement = getDqlQueryStatement(queryArgument);
            if (dqlStatement != null) {
                DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(queryArgument.getDataSourceId());
                if (dataSourceModel != null) {
                    // TODO Should we cache this (dqlStatement => read fields)?
                    DqlStatement<Object> parsedStatement = dataSourceModel.parseStatement(dqlStatement);
                    if (parsedStatement instanceof Select) {
                        CollectOptions collectOptions = new CollectOptions()
                                .setFilterPersistentTerms(true)
                                .setTraverseSelect(true)
                                .setTraverseSqlExpressible(true);
                        parsedStatement.collect(collectOptions);
                        SchemaScopeBuilder ssb = SchemaScope.builder();
                        for (Expression<Object> term : collectOptions.getCollectedTerms()) {
                            if (term instanceof DomainField domainField) {
                                ssb.addField(domainField.getDomainClass().getId(), domainField.getId());
                            }
                        }
                        SchemaScope querySchemaScope = ssb.build();
                        dev.webfx.stack.db.query.QueryArgumentBuilder queryArgumentBuilder = QueryArgument.builder().copy(queryArgument).addDataScope(querySchemaScope);
                        // Partition scope from the where clause (ex: event=$2 → this
                        // subscription only shows event 1857's rows): the pulse can then
                        // skip it for modifications provably belonging to other
                        // partitions — see DqlScopeUtil for the soundness rules.
                        AggregateScopeBuilder asb = AggregateScope.builder();
                        Object queriedClassId = parsedStatement.getDomainClass() instanceof dev.webfx.stack.orm.domainmodel.DomainClass
                                ? ((dev.webfx.stack.orm.domainmodel.DomainClass) parsedStatement.getDomainClass()).getId()
                                : dataSourceModel.getDomainModel().getClass(parsedStatement.getDomainClass()).getId();
                        if (DqlScopeUtil.addPartitions(asb, parsedStatement.getWhere(), queryArgument.getParameters(), null, queriedClassId) > 0)
                            queryArgumentBuilder.addDataScope(asb.build());
                        queryArgument = queryArgumentBuilder.build();
                        argument = QueryPushArgument.builder().copy(argument).setQueryArgument(queryArgument).build();
                    }
                }
            }
        }
        return targetProvider.executeQueryPush(argument);
    }

    private static String getDqlQueryStatement(QueryArgument argument) {
        QueryArgument originalArgument = argument.getOriginalArgument();
        return "DQL".equalsIgnoreCase(argument.getLanguage()) ? argument.getStatement()
                : originalArgument != null && originalArgument != argument ? getDqlQueryStatement(originalArgument)
                : null;
    }

}
