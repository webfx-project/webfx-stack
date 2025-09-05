package dev.webfx.stack.orm.dql.querypush.interceptor;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.datascope.schema.SchemaScope;
import dev.webfx.stack.db.datascope.schema.SchemaScopeBuilder;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.querypush.PulseArgument;
import dev.webfx.stack.db.querypush.QueryPushArgument;
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
                        queryArgument = QueryArgument.builder().copy(queryArgument).addDataScope(querySchemaScope).build();
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
