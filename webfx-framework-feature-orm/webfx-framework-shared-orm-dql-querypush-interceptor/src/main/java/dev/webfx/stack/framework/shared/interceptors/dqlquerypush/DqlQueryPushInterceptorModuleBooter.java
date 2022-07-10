package dev.webfx.stack.framework.shared.interceptors.dqlquerypush;

import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.framework.shared.orm.domainmodel.DomainField;
import dev.webfx.stack.framework.shared.orm.expression.CollectOptions;
import dev.webfx.stack.framework.shared.orm.expression.Expression;
import dev.webfx.stack.framework.shared.orm.expression.terms.DqlStatement;
import dev.webfx.stack.framework.shared.orm.expression.terms.Select;
import dev.webfx.stack.framework.shared.services.datasourcemodel.DataSourceModelService;
import dev.webfx.stack.framework.shared.services.querypush.PulseArgument;
import dev.webfx.stack.framework.shared.services.querypush.QueryPushArgument;
import dev.webfx.stack.framework.shared.services.querypush.spi.QueryPushServiceProvider;
import dev.webfx.stack.platform.shared.datascope.schema.SchemaScope;
import dev.webfx.stack.platform.shared.datascope.schema.SchemaScopeBuilder;
import dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.platform.shared.services.datasource.LocalDataSourceService;
import dev.webfx.stack.platform.shared.services.query.QueryArgument;
import dev.webfx.stack.platform.async.Future;
import dev.webfx.platform.shared.util.serviceloader.SingleServiceProvider;

/**
 * @author Bruno Salmon
 */
public class DqlQueryPushInterceptorModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-framework-shared-orm-dql-querypush-interceptor";
    }

    @Override
    public int getBootLevel() {
        return APPLICATION_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
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
                            if (term instanceof DomainField) {
                                DomainField domainField = (DomainField) term;
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
