package dev.webfx.stack.orm.dql.submit.interceptor;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.datascope.KeyDataScope;
import dev.webfx.stack.db.datascope.MultiKeyDataScope;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.datascope.aggregate.AggregateScopeBuilder;
import dev.webfx.stack.db.datascope.schema.SchemaScope;
import dev.webfx.stack.db.datascope.schema.SchemaScopeBuilder;
import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.orm.expression.terms.*;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public class DqlSubmitInterceptorModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-orm-dql-submit-interceptor";
    }

    @Override
    public int getBootLevel() {
        return APPLICATION_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // The purpose of this interceptor is to automatically translate DQL to SQL and compute the schema scope when
        // the submit reaches its local data source (works only with DQL)
        SingleServiceProvider.registerServiceInterceptor(SubmitServiceProvider.class, targetProvider ->
                new SubmitServiceProvider() {
                    @Override
                    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
                        return interceptAndExecuteSubmit(argument, targetProvider);
                    }
                    @Override
                    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
                        return interceptAndExecuteSubmitBatch(batch, targetProvider);
                    }
                });
    }

    private static Future<SubmitResult> interceptAndExecuteSubmit(SubmitArgument argument, SubmitServiceProvider targetProvider) {
        return targetProvider.executeSubmit(translateSubmit(argument));
    }

    private static Future<Batch<SubmitResult>> interceptAndExecuteSubmitBatch(Batch<SubmitArgument> batch, SubmitServiceProvider targetProvider) {
        return targetProvider.executeSubmitBatch(translateBatch(batch));
    }

    private static SubmitArgument translateSubmit(SubmitArgument argument) {
        String language = argument.getLanguage();
        Object dataSourceId = argument.getDataSourceId();
        if (language != null && LocalDataSourceService.isDataSourceLocal(dataSourceId)) {
            DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
            if (dataSourceModel != null) {
                String statement = argument.getStatement(); // can be DQL or SQL
                String sqlStatement = dataSourceModel.translateStatementIfDql(language, statement);
                if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                    //Logger.log("Translated to: " + sqlStatement);
                    argument = SubmitArgument.builder().copy(argument)
                            .setLanguage(null).setStatement(sqlStatement)
                            .addDataScope(createDataScope(statement, dataSourceModel, argument.getParameters()))
                            .build();
                }
            }
        }
        return argument;
    }

    private static Batch<SubmitArgument> translateBatch(Batch<SubmitArgument> batch) {
        return new Batch<>(Arrays.stream(batch.getArray()).map(DqlSubmitInterceptorModuleBooter::translateSubmit).toArray(SubmitArgument[]::new));
    }

    private static DataScope createDataScope(String dqlSubmitStatement, DataSourceModel dataSourceModel, Object[] parameters) {
        // Returning a wrapper so the scope computation can be skipped if not used later
        // (ex: if intersects method is never called or submit fails)
        return new MultiKeyDataScope() {

            private KeyDataScope[] keyDataScopes;

            @Override
            public KeyDataScope[] getKeyDataScopes() { // Called only if get used
                if (keyDataScopes == null) {
                    DqlStatement<Object> dqlStatement = dataSourceModel.parseStatement(dqlSubmitStatement);
                    // Building the schema and aggregate scope
                    SchemaScopeBuilder ssb = SchemaScope.builder();
                    AggregateScopeBuilder asb = AggregateScope.builder();
                    if (dqlStatement instanceof Update) { // Update => only fields in set clause are impacted
                        for (Expression<?> expression : ((Update<Object>) dqlStatement).getSetClause().getExpressions()) {
                            if (expression instanceof Equals) {
                                Equals<?> equals = (Equals<?>) expression;
                                Expression<?> left = equals.getLeft();
                                if (left instanceof DomainField) {
                                    DomainField domainField = (DomainField) left;
                                    ssb.addField(domainField.getDomainClass().getId(), domainField.getId());
                                    DomainClass foreignClass = domainField.getForeignClass();
                                    if (foreignClass != null && foreignClass.isAggregate()) {
                                        Expression<?> right = equals.getRight();
                                        Object value = null;
                                        if (right instanceof Constant)
                                            value = ((Constant<?>) right).getConstantValue();
                                        else if (right instanceof Parameter)
                                            value = parameters[0]; // TODO compute the correct parameter index
                                        if (value != null)
                                            asb.addAggregate(foreignClass.getName(), value);
                                    }
                                }
                            }
                        }
                    } else { // Insert or Delete => all fields are impacted
                        DomainClass domainClass = dqlStatement.getDomainClass() instanceof DomainClass ? (DomainClass) dqlStatement.getDomainClass()
                                : dataSourceModel.getDomainModel().getClass(dqlStatement.getDomainClass());
                        ssb.addClass(domainClass.getId());
                    }
                    SchemaScope schemaScope = ssb.build();
                    AggregateScope aggregateScope = asb.build();
                    // Putting the scopes into the array
                    keyDataScopes = new KeyDataScope[] { schemaScope, aggregateScope };
                }
                return keyDataScopes;
            }
        };
    }
}
