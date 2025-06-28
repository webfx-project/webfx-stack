package dev.webfx.stack.orm.reactive.dql.statement;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.function.Converter;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.dql.DqlStatementBuilder;
import dev.webfx.stack.orm.reactive.dql.statement.conventions.*;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.webfx.stack.orm.dql.DqlStatement.limit;

/**
 * @author Bruno Salmon
 */
public final class ReactiveDqlStatement<E> implements ReactiveDqlStatementAPI<E, ReactiveDqlStatement<E>> {

    // Entity Java class (not used in the code so far, just passed in the constructor to type E)
    private Class<E> domainJavaClass;
    // Domain class ID taken from DqlStatements (they should all have the same domain class ID)
    // Note: it's the responsibility of the developer to make domainClassId and domainJavaClass match)
    private Object domainClassId;
    // The list of DqlStatement properties (each DqlStatement value may change in reaction to other JavaFX properties such as user interface)
    private final List<ObservableValue<DqlStatement>> dqlStatementProperties = new ArrayList<>();
    // The base statement is the fist sample of dql statements giving the domain class ID
    private DqlStatement baseStatement;
    private final ObjectProperty<DqlStatement> resultingDqlStatementProperty = new SimpleObjectProperty<>();
    private Scheduled dqlStatementChangedScheduled;
    private final List<Function<DqlStatement, DqlStatement>> resultTransformers = new ArrayList<>();

    /*==================================================================================================================
      ============================================== Constructors ======================================================
      ================================================================================================================*/

    public ReactiveDqlStatement() {}

    public ReactiveDqlStatement(Class<E> domainJavaClass) {
        this.domainJavaClass = domainJavaClass;
    }

    @Override
    public ReactiveDqlStatement<E> getReactiveDqlStatement() {
        return this;
    }

    @Override
    public Object getDomainClassId() {
        fetchBaseStatementAndDomainClassIdIfNecessary();
        return domainClassId;
    }

    @Override
    public DqlStatement getBaseStatement() {
        fetchBaseStatementAndDomainClassIdIfNecessary();
        return baseStatement;
    }

    @Override
    public DqlStatement getResultingDqlStatement() {
        return resultingDqlStatementProperty.get();
    }

    @Override
    public ObservableValue<DqlStatement> resultingDqlStatementProperty() {
        return resultingDqlStatementProperty;
    }

    @Override
    public void addResultTransformer(Function<DqlStatement, DqlStatement> resultTransformer) {
        resultTransformers.add(resultTransformer);
    }

    private void fetchBaseStatementAndDomainClassIdIfNecessary() {
        if (baseStatement == null || dqlStatementChangedScheduled != null) {
            synchronized (dqlStatementProperties) { // to avoid ConcurrentModificationException if another thread wants to add another statement
                for (ObservableValue<DqlStatement> dqlStatementProperty : dqlStatementProperties) {
                    DqlStatement dqlStatement = dqlStatementProperty.getValue();
                    if (dqlStatement != null)
                        domainClassId = dqlStatement.getDomainClassId();
                    if (domainClassId != null) {
                        baseStatement = dqlStatement;
                        break;
                    }
                }
            }
        }
    }

    private void markDqlStatementsAsChanged() {
        if (dqlStatementChangedScheduled == null) {
            dqlStatementChangedScheduled = UiScheduler.scheduleDeferred(this::recomputeResultingDqlStatement);
        }
    }

    private void recomputeResultingDqlStatement() {
        dqlStatementChangedScheduled = null;
        DqlStatement result = mergeDqlStatements();
        resultingDqlStatementProperty.setValue(result);
    }

    private DqlStatement mergeDqlStatements() {
        DqlStatementBuilder mergeBuilder = new DqlStatementBuilder(getDomainClassId());
        synchronized (dqlStatementProperties) { // to avoid ConcurrentModificationException if another thread wants to add another statement
            for (ObservableValue<DqlStatement> dqlStatementProperty : dqlStatementProperties)
                mergeBuilder.merge(dqlStatementProperty.getValue());
        }
        DqlStatement result = mergeBuilder.build();
        for (Function<DqlStatement, DqlStatement> resultTransformer : resultTransformers)
            if (!result.isInherentlyEmpty())
                result = resultTransformer.apply(result);
        return result;
    }


    /*==================================================================================================================
      ============================================== Fluent API ========================================================
      ================================================================================================================*/

    public ReactiveDqlStatement<E> always(ObservableValue<DqlStatement> dqlStatementProperty) {
        FXProperties.runOnPropertyChange(this::markDqlStatementsAsChanged, dqlStatementProperty);
        return addWithoutListening(dqlStatementProperty);
    }

    private ReactiveDqlStatement<E> addWithoutListening(ObservableValue<DqlStatement> dqlStatementProperty) {
        synchronized (dqlStatementProperties) { // to avoid ConcurrentModificationException if mergeDqlStatements() is also executed
            dqlStatementProperties.add(dqlStatementProperty);
        }
        markDqlStatementsAsChanged();
        return this;
    }

    @Override
    public ReactiveDqlStatement<E> always(DqlStatement dqlStatement) {
        return addWithoutListening(new SimpleObjectProperty<>(dqlStatement));
    }

    @Override
    public ReactiveDqlStatement<E> always(String dqlStatementString) {
        return always(DqlStatement.parse(dqlStatementString));
    }

    @Override
    public ReactiveDqlStatement<E> always(ReadOnlyAstObject json) {
        return always(new DqlStatement(json));
    }

    @Override
    public ReactiveDqlStatement<E> always(Object jsonOrClass) {
        return always(new DqlStatementBuilder(jsonOrClass).build());
    }

    @Override
    public <T> ReactiveDqlStatement<E> always(ObservableValue<T> property, Converter<T, DqlStatement> toDqlStatementConverter) {
        return addWithoutListening(property.map(t -> {
            // Calling the converter to get the dql statement
            DqlStatement dqlStatement = toDqlStatementConverter.convert(t);
            // If different from last value, this will trigger a global change check
            // However it's possible that the DqlStatement hasn't changed but contains parameters that have changed (ex: name like ?search)
            // In that case (DqlStatement with parameter), we always schedule a global change check (which will consider parameters)
            //if (dqlStatementString != null && dqlStatementString.contains("?")) // Simple parameter test with ?
            markDqlStatementsAsChanged();
            return dqlStatement;
        }));
    }

    private <T> Converter<T, DqlStatement> stringToDqlStatementConverter(Converter<T, String> toDqlStatementStringConverter) {
        return t -> DqlStatement.parse(toDqlStatementStringConverter.convert(t));
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifEquals(ObservableValue<T> property, T value, DqlStatement dqlStatement) {
        return always(property, v -> Objects.equals(v, value) ? dqlStatement : null);
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifEquals(ObservableValue<T> property, T value, Supplier<DqlStatement> dqlStatementSupplier) {
        return always(property, v -> Objects.equals(v, value) ? dqlStatementSupplier.get() : null);
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifNotEquals(ObservableValue<T> property, T value, DqlStatement dqlStatement) {
        return always(property, v -> !Objects.equals(v, value) ? dqlStatement : null);
    }

    public ReactiveDqlStatement<E> ifTrue(ObservableValue<Boolean> ifProperty, DqlStatement dqlStatement) {
        return ifEquals(ifProperty, Boolean.TRUE, dqlStatement);
    }

    @Override
    public ReactiveDqlStatement<E> ifTrue(ObservableValue<Boolean> ifProperty, String dqlStatementString) {
        return ifTrue(ifProperty, DqlStatement.parse(dqlStatementString));
    }

    @Override
    public ReactiveDqlStatement<E> ifFalse(ObservableValue<Boolean> ifProperty, DqlStatement dqlStatement) {
        return ifEquals(ifProperty, Boolean.FALSE, dqlStatement);
    }

    public ReactiveDqlStatement<E> ifFalse(ObservableValue<Boolean> ifProperty, String dqlStatementString) {
        return ifFalse(ifProperty, DqlStatement.parse(dqlStatementString));
    }


    @Override
    public <T extends Number> ReactiveDqlStatement<E> ifPositive(ObservableValue<T> property, Converter<T, DqlStatement> toDqlStatementConverter) {
        return always(property, value -> Numbers.isPositive(value) ? toDqlStatementConverter.convert(value) : null);
    }

    @Override
    public ReactiveDqlStatement<E> ifNotEmpty(ObservableValue<String> property, Converter<String, DqlStatement> toDqlStatementConverter) {
        return always(property, s -> Strings.isEmpty(s) ? null : toDqlStatementConverter.convert(s));
    }

    @Override
    public ReactiveDqlStatement<E> ifTrimNotEmpty(ObservableValue<String> property, Converter<String, DqlStatement> toDqlStatementConverter) {
        return ifNotEmpty(property, s -> toDqlStatementConverter.convert(Strings.trim(s)));
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifNotNull(ObservableValue<T> property, Converter<T, DqlStatement> toDqlStatementConverter) {
        return ifNotNullOtherwise(property, toDqlStatementConverter, null);
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifNotNullOtherwise(ObservableValue<T> property, Converter<T, DqlStatement> toDqlStatementConverter, DqlStatement otherwiseDqlStatement) {
        return always(property, value -> value == null ? otherwiseDqlStatement : toDqlStatementConverter.convert(value));
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifNotNullOtherwiseEmpty(ObservableValue<T> property, Converter<T, DqlStatement> toDqlStatementConverter) {
        return ifNotNullOtherwise(property, toDqlStatementConverter, DqlStatement.EMPTY_STATEMENT);
    }

    @Override
    public <T> ReactiveDqlStatement<E> ifNotNullOtherwiseEmptyString(ObservableValue<T> property, Converter<T, String> toDqlStatementStringConverter) {
        return ifNotNullOtherwiseEmpty(property, stringToDqlStatementConverter(toDqlStatementStringConverter));
    }

    @Override
    public <T, T2 extends T> ReactiveDqlStatement<E> ifInstanceOf(ObservableValue<T> property, Class<T2> clazz, Converter<T2, DqlStatement> toDqlStatementConverter) {
        return addWithoutListening(property.map(v -> {
            markDqlStatementsAsChanged();
            return dev.webfx.platform.util.Objects.isInstanceOf(v, clazz) ? toDqlStatementConverter.convert((T2) v) : null;
        }));
    }

    /*==================================================================================================================
      ======================================= Classic static factory API ===============================================
      ================================================================================================================*/

    public static <E> ReactiveDqlStatement<E> create() {
        return create(null);
    }

    public static <E> ReactiveDqlStatement<E> create(Class<E> domainJavaClass) {
        return new ReactiveDqlStatement<>(domainJavaClass);
    }

    /*==================================================================================================================
      ==================================== Conventional static factory API =============================================
      ================================================================================================================*/

    public static <E> ReactiveDqlStatement<E> createMaster(Object pm) {
        return createMaster(null, pm);
    }

    public static <E> ReactiveDqlStatement<E> createMaster(Class<E> domainJavaClass, Object pm) {
        return initializeMaster(create(domainJavaClass), pm);
    }

    protected static <E> ReactiveDqlStatement<E> initializeMaster(ReactiveDqlStatement<E> master, Object pm) {
        // Applying the condition and columns selected by the user
        if (pm instanceof HasConditionDqlStatementProperty)
            master.ifNotNullOtherwiseEmpty(((HasConditionDqlStatementProperty) pm).conditionDqlStatementProperty(), conditionDqlStatement -> conditionDqlStatement);
        if (pm instanceof HasColumnsDqlStatementProperty)
            master.ifNotNullOtherwiseEmpty(((HasColumnsDqlStatementProperty) pm).columnsDqlStatementProperty(), columnsDqlStatement -> columnsDqlStatement);
        // Also, in case groups are showing and a group is selected, applying the condition associated with that group
        if (pm instanceof HasSelectedGroupConditionDqlStatementProperty)
            master.ifNotNull(((HasSelectedGroupConditionDqlStatementProperty) pm).selectedGroupConditionDqlStatementProperty(), selectedGroupConditionDqlStatement -> selectedGroupConditionDqlStatement);
        // Limit clause
        if (pm instanceof HasLimitProperty)
            master.ifPositive(((HasLimitProperty) pm).limitProperty(), limit -> limit("?", limit));
        return master;
    }

    public static <E> ReactiveDqlStatement<E> createGroup(Object pm) {
        return createGroup(null, pm);
    }

    public static <E> ReactiveDqlStatement<E> createGroup(Class<E> domainJavaClass, Object pm) {
        return initializeGroup(create(domainJavaClass), pm);
    }

    protected static <E> ReactiveDqlStatement<E> initializeGroup(ReactiveDqlStatement<E> group, Object pm) {
        // Applying the condition and group selected by the user
        if (pm instanceof HasConditionDqlStatementProperty)
            group.ifNotNullOtherwiseEmpty(((HasConditionDqlStatementProperty) pm).conditionDqlStatementProperty(), conditionDqlStatement -> conditionDqlStatement);
        if (pm instanceof HasGroupDqlStatementProperty)
            group.ifNotNullOtherwiseEmpty(((HasGroupDqlStatementProperty) pm).groupDqlStatementProperty(), groupDqlStatement -> groupDqlStatement.getGroupBy() != null ? groupDqlStatement : DqlStatement.EMPTY_STATEMENT);
        return group;
    }

    public static <E> ReactiveDqlStatement<E> createSlave(Object pm) {
        return createSlave(null, pm);
    }

    public static <E> ReactiveDqlStatement<E> createSlave(Class<E> domainJavaClass, Object pm) {
        return initializeSlave(create(domainJavaClass), pm);
    }

    protected static <E> ReactiveDqlStatement<E> initializeSlave(ReactiveDqlStatement<E> slave, Object pm) {
        if (pm instanceof HasSelectedMasterProperty)
            slave.ifTrue(((HasSelectedMasterProperty) pm).selectedMasterProperty().map(selectedMaster ->
                    selectedMaster == null || pm instanceof HasSlaveVisibilityCondition && !((HasSlaveVisibilityCondition) pm).isSlaveVisible(selectedMaster)
            ), DqlStatement.EMPTY_STATEMENT);
        return slave;
    }
}
