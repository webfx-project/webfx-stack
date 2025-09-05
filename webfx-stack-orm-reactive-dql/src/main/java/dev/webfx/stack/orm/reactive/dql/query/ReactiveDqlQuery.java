package dev.webfx.stack.orm.reactive.dql.query;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.shareddata.cache.CacheEntry;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.function.Converter;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.domainmodel.HasDataSourceModel;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.entity.DqlQueries;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.reactive.call.ReactiveCall;
import dev.webfx.stack.orm.reactive.call.query.ReactiveQueryCall;
import dev.webfx.stack.orm.reactive.dql.statement.ReactiveDqlStatement;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.HasActiveProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public class ReactiveDqlQuery<E> implements ReactiveDqlQueryAPI<E, ReactiveDqlQuery<E>> {

    private final ReactiveDqlStatement<E> reactiveDqlStatement;
    protected final ReactiveCall<QueryArgument, QueryResult> reactiveQueryCall;
    private DataSourceModel dataSourceModel;
    private SqlCompiled sqlCompiled;

    public ReactiveDqlQuery(ReactiveDqlStatement<E> reactiveDqlStatement) {
        this(reactiveDqlStatement, new ReactiveQueryCall());
    }

    public ReactiveDqlQuery(ReactiveDqlStatement<E> reactiveDqlStatement, ReactiveCall<QueryArgument, QueryResult> reactiveQueryCall) {
        this.reactiveDqlStatement = reactiveDqlStatement;
        this.reactiveQueryCall = reactiveQueryCall;
        FXProperties.runOnPropertyChange(this::updateQueryArgument, resultingDqlStatementProperty());
    }

    @Override
    public ReactiveDqlQuery<E> getReactiveDqlQuery() {
        return this;
    }

    @Override
    public ReactiveDqlStatement<E> getReactiveDqlStatement() {
        return reactiveDqlStatement;
    }

    @Override
    public ReactiveDqlQuery<E> setActiveParent(ReactiveDqlQueryAPI<?, ?> activeParent) {
        return this;
    }

    @Override
    public DataSourceModel getDataSourceModel() {
        return dataSourceModel;
    }

    /*==================================================================================================================
      ============================================== Fluent API ========================================================
      ================================================================================================================*/

    private AggregateScope aggregateScope;

    @Override
    public <T> ReactiveDqlQuery<E> setAggregateScope(ObservableValue<T> property, Converter<T, AggregateScope> toAggregateScopeConverter) {
        FXProperties.runNowAndOnPropertyChange(value -> aggregateScope = toAggregateScopeConverter.convert(value), property);
        return this;
    }


    @Override
    public ReactiveDqlQuery<E> setDataSourceModel(DataSourceModel dataSourceModel) {
        this.dataSourceModel = dataSourceModel;
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> bindActivePropertyTo(ObservableValue<Boolean> activeProperty) {
        reactiveQueryCall.bindActivePropertyTo(activeProperty);
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> unbindActiveProperty() {
        reactiveQueryCall.unbindActiveProperty();
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> setResultCacheEntry(String cacheEntryKey) {
        reactiveQueryCall.setResultCacheEntry(cacheEntryKey);
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> setResultCacheEntry(CacheEntry<Pair<QueryArgument, QueryResult>> resultCacheEntry) {
        reactiveQueryCall.setResultCacheEntry(resultCacheEntry);
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> start() {
        reactiveQueryCall.start();
        return this;
    }

    @Override
    public ReactiveDqlQuery<E> stop() {
        reactiveQueryCall.stop();
        return this;
    }

    @Override
    public boolean isStarted() {
        return reactiveQueryCall.isStarted();
    }

    @Override
    public void refreshWhenActive() {
        // UPDATE 21/07/2025: changed forced from false to true, otherwise upcomingOrdersMapper.refreshWhenActive();
        // was not doing anything in OrdersActivity. TODO remove this comment if no side effect
        reactiveQueryCall.refreshWhenReady(true);
    }

    @Override
    public final ObservableValue<QueryResult> resultProperty() {
        return reactiveQueryCall.resultProperty();
    }

    @Override
    public final BooleanProperty activeProperty() {
        return reactiveQueryCall.activeProperty();
    }

    @Override
    public void setActive(boolean active) {
        reactiveQueryCall.setActive(active);
    }

    protected void updateQueryArgument(DqlStatement dqlStatement) {
        //Console.log("ReactiveDqlQuery.updateQueryArgument()");
        // Shortcut: when the dql statement is inherently empty, we return an empty entity list immediately (no server call) - unless we are in push mode already registered on the server
        if (dqlStatement.isInherentlyEmpty()) {
            reactiveQueryCall.setArgument(null);
            return;
        }
        // Generating the query argument
        QueryArgument queryArgument = createQueryArgument(dqlStatement.toDqlSelect(), dqlStatement.getSelectParameterValues());
        // Skipping the server call if there is no difference in the parameters compared to the last call
        if (isDifferentFromLastQuery(queryArgument)) {
            //Console.log("Setting queryArgument = " + queryArgument);
            reactiveQueryCall.setArgument(queryArgument);
        } else
            Console.log("No difference with previous query");
    }

    private String dqlQuery;

    private QueryArgument createQueryArgument(String dqlQuery, Object[] parameters) {
        this.dqlQuery = dqlQuery;
        sqlCompiled = null;
        return DqlQueries.newQueryArgument(getDataSourceId(), aggregateScope, dqlQuery, parameters);
    }

    @Override
    public SqlCompiled getSqlCompiled() {
        if (sqlCompiled == null && dqlQuery != null)
            sqlCompiled = getDataSourceModel().parseAndCompileSelect(dqlQuery);
        return sqlCompiled;
    }

    private boolean isDifferentFromLastQuery(QueryArgument queryArgument) {
        return reactiveQueryCall.hasArgumentChangedSinceLastCall(queryArgument);
    }

    public void executeParsingCode(Runnable parsingCode) {
        ThreadLocalReferenceResolver.executeCodeInvolvingReferenceResolver(parsingCode, getRootAliasReferenceResolver());
    }

    @Override
    public DomainClass getDomainClass() {
        return getDomainModel().getClass(getDomainClassId());
    }

    @Override
    public Object getDomainClassId() {
        return getReactiveDqlStatement().getDomainClassId();
    }

    private ReferenceResolver rootAliasReferenceResolver;
    private DqlStatement rootAliasBaseStatement; // Keeping a reference to know if the resolver cache needs to be cleared

    @Override
    public ReferenceResolver getRootAliasReferenceResolver() {
        if (rootAliasReferenceResolver == null || rootAliasBaseStatement != getBaseStatement()) {
            // Before parsing, we prepare a ReferenceResolver to resolve possible references to root aliases
            Map<String, Alias<?>> rootAliases = new HashMap<>();
            rootAliasReferenceResolver = rootAliases::get;
            DqlStatement baseStatement = rootAliasBaseStatement = getBaseStatement();
            if (baseStatement != null) { // Root aliases are stored in the baseStatement
                // TODO: investigate if DqlStatement should rather implement ReferenceResolver (like SqlStatementBuilder does)
                // The first possible root alias is the base statement alias. Ex: Event e => the alias "e" then acts in a
                // similar way as "this" in java because it refers to the current Event row in the select, so some
                // expressions such as sub queries may refer to it (ex: select count(1) from Booking where event=e)
                String alias = baseStatement.getAlias();
                if (alias != null) // when defined, we add an Alias expression that can be returned when resolving this alias
                    rootAliases.put(alias, new Alias<>(alias, getDomainClass()));
                // Other possible root aliases can be As expressions defined in the base filter fields, such as subqueries.
                // For example, if the fields contain (select ...) as xxx -> then xxx can be referenced in expression columns
                String fields = baseStatement.getFields();
                if (Strings.contains(fields, " as ")) { // quick skipping if the fields don't contain " as "
                    executeParsingCode(() -> {
                        DomainModel domainModel = getDomainModel();
                        Object domainClassId = getDomainClassId();
                        for (Expression<?> field : domainModel.parseExpressionArray(fields, domainClassId).getExpressions()) {
                            if (field instanceof As) { // If a field is an As expression,
                                As<?> as = (As<?>) field;
                                // we add an Alias expression that can be returned when resolving this alias
                                rootAliases.put(as.getAlias(), new Alias<>(as.getAlias(), as.getType()));
                            }
                        }
                    });
                }
            }
        }
        return rootAliasReferenceResolver;
    }

    /*==================================================================================================================
      ======================================= Classic static factory API ===============================================
      ================================================================================================================*/

    public static <E> ReactiveDqlQuery<E> create(ReactiveDqlStatement<E> reactiveDqlStatement) {
        return new ReactiveDqlQuery<>(reactiveDqlStatement);
    }

    /*==================================================================================================================
      ==================================== Conventional static factory API =============================================
      ================================================================================================================*/


    public static <E> ReactiveDqlQuery<E> createReactiveChain() {
        return create(ReactiveDqlStatement.create());
    }

    public static <E> ReactiveDqlQuery<E> createReactiveChain(Object mixin) {
        return initMixin(create(ReactiveDqlStatement.create()), mixin);
    }

    // Master

    public static <E> ReactiveDqlQuery<E> createMasterReactiveChain(Object pm) {
        return create(ReactiveDqlStatement.createMaster(pm));
    }

    public static <E> ReactiveDqlQuery<E> createMasterReactiveChain(Object mixin, Object pm) {
        return initMixin(create(ReactiveDqlStatement.createMaster(pm)), mixin);
    }

    // Group

    public static <E> ReactiveDqlQuery<E> createGroupReactiveChain(Object pm) {
        return create(ReactiveDqlStatement.createGroup(pm));
    }

    public static <E> ReactiveDqlQuery<E> createGroupReactiveChain(Object mixin, Object pm) {
        return initMixin(create(ReactiveDqlStatement.createGroup(pm)), mixin);
    }

    // Slave

    public static <E> ReactiveDqlQuery<E> createSlaveReactiveChain(Object pm) {
        return create(ReactiveDqlStatement.createSlave(pm));
    }

    public static <E> ReactiveDqlQuery<E> createSlaveReactiveChain(Object mixin, Object pm) {
        return initMixin(create(ReactiveDqlStatement.createSlave(pm)), mixin);
    }

    protected static <E, RDQ extends ReactiveDqlQuery<E>> RDQ initMixin(RDQ instance, Object mixin) {
        if (mixin instanceof HasDataSourceModel)
            instance.setDataSourceModel(((HasDataSourceModel) mixin).getDataSourceModel());
        if (mixin instanceof HasActiveProperty)
            instance.bindActivePropertyTo(((HasActiveProperty) mixin).activeProperty());
        return instance;
    }
}
