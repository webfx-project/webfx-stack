package dev.webfx.stack.orm.reactive.entities.entities_to_grid;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.extras.i18n.I18n;
import dev.webfx.stack.orm.reactive.dql.query.ReactiveDqlQuery;
import dev.webfx.stack.orm.reactive.dql.statement.ReactiveDqlStatement;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.dql.DqlStatementBuilder;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public abstract class ReactiveGridMapper<E extends Entity> {

    protected final ReactiveEntitiesMapper<E> reactiveEntitiesMapper;

    protected EntityColumn<E>[] entityColumns;
    protected boolean autoSelectSingleRow;
    protected boolean selectFirstRowOnFirstDisplay;
    private boolean applyDomainModelRowStyle;
    protected boolean startsWithEmptyTable = true;
    protected Consumer<E> selectedEntityHandler;
    private String columnsPersistentFields;

    public ReactiveGridMapper(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        this.reactiveEntitiesMapper = reactiveEntitiesMapper;
        reactiveEntitiesMapper.addEntitiesHandler(this::onEntityListChanged);
        ReactiveDqlStatement<E> reactiveDqlStatement = reactiveEntitiesMapper.getReactiveDqlQuery().getReactiveDqlStatement();
        reactiveDqlStatement.addResultTransformer(dqlStatement -> {
            // If the resulting dql statement defines columns (ex: from a fields filter), we take them as the columns to display
            if (dqlStatement.getColumns() != null)
                setEntityColumnsPrivate(parseEntityColumnsDefinition(dqlStatement.getColumns()));
            // If no columns have been defined so far (neither from explicit setEntityColumns() call nor from resulting dql statement)
            if (entityColumns == null)
                return DqlStatement.EMPTY_STATEMENT; // We prevent the server call as there is nothing to show
            updateColumnsPersistentFields();
            // If as a result of applying the columns we have additional persistent fields to load, we include them in the final dql statement
            if (columnsPersistentFields != null)
                dqlStatement = new DqlStatementBuilder(dqlStatement).mergeFields(columnsPersistentFields).build();
            scheduleEmptyTable();
            return dqlStatement;
        });
        // Reacting to the change of i18n dictionary TODO: make this configurable without forcing i18n dependency
        FXProperties.runOnPropertyChange(() -> onEntityListChanged(getCurrentEntities()), I18n.dictionaryProperty());
        //reactiveDqlStatement.combine(persistentFieldsDqlStatementProperty);
    }

    public ReactiveEntitiesMapper<E> getReactiveEntitiesMapper() {
        return reactiveEntitiesMapper;
    }

    public EntityColumn<E>[] getEntityColumns() {
        return entityColumns;
    }

    public EntityList<E> getCurrentEntities() {
        return reactiveEntitiesMapper.getCurrentEntities();
    }

    public abstract List<E> getSelectedEntities();

    public abstract E getSelectedEntity();

    public ReactiveGridMapper<E> autoSelectSingleRow() {
        autoSelectSingleRow = true;
        return this;
    }

    protected ReactiveGridMapper<E> selectFirstRowOnFirstDisplay() {
        selectFirstRowOnFirstDisplay = true;
        return this;
    }

    public ReactiveGridMapper<E> setSelectedEntityHandler(Consumer<E> selectedEntityHandler) {
        this.selectedEntityHandler = selectedEntityHandler;
        return this;
    }

    protected EntityColumnFactory getEntityColumnFactory() {
        return EntityColumnFactory.get();
    }

    public ReactiveGridMapper<E> setEntityColumns(String jsonArrayOrExpressionDefinition) {
        setEntityColumns(parseEntityColumnsDefinition(jsonArrayOrExpressionDefinition));
        return this;
    }

    @SuppressWarnings("unchecked")
    private EntityColumn<E>[] parseEntityColumnsDefinition(String jsonArrayOrExpressionDefinition) {
        ReactiveDqlQuery<E> reactiveDqlQuery = reactiveEntitiesMapper.getReactiveDqlQuery();
        Object[] holder = new Object[1];
        reactiveDqlQuery.executeParsingCode(() -> holder[0] = getEntityColumnFactory().fromJsonArrayOrExpressionsDefinition(jsonArrayOrExpressionDefinition, reactiveDqlQuery.getDomainModel(), reactiveDqlQuery.getReactiveDqlStatement().getDomainClassId()));
        return (EntityColumn<E>[]) holder[0];
    }

    public ReactiveGridMapper<E> setEntityColumns(EntityColumn<E>... entityColumns) {
        setEntityColumnsPrivate(entityColumns);
        // Asking a dql refresh
        // reactiveDqlStatement.refreshResultTransform();
        return this;
    }

    private void setEntityColumnsPrivate(EntityColumn<E>[] entityColumns) {
        this.entityColumns = entityColumns;
        markColumnsPersistentFieldsAsOutOfDate();
        if (startsWithEmptyTable && reactiveEntitiesMapper.getEntities() == null)
            scheduleEmptyTable();
    }

    private boolean emptyTableScheduled;

    private void scheduleEmptyTable() {
        if (!emptyTableScheduled) {
            emptyTableScheduled = true;
            Platform.runLater(() -> {
                //emptyTableScheduled = false; // Commented as doing it only once
                if (startsWithEmptyTable && reactiveEntitiesMapper.getEntities() == null)
                    onEntityListChanged(null);
            });
        }
    }

    public ReactiveGridMapper<E> applyDomainModelRowStyle() {
        applyDomainModelRowStyle = true;
        markColumnsPersistentFieldsAsOutOfDate();
        return this;
    }

    private void markColumnsPersistentFieldsAsOutOfDate() {
        columnsPersistentFields = null;
    }

    private void updateColumnsPersistentFields() {
        if (columnsPersistentFields == null) {
            if (applyDomainModelRowStyle)
                applyDomainModelRowStyleNow();
            collectColumnsPersistentFields();
        }
    }

    private void applyDomainModelRowStyleNow() {
        DomainClass domainClass = getDomainClass();
        ExpressionArray<E> rowStylesExpressionArray = domainClass.getStyleClassesExpressionArray();
        if (rowStylesExpressionArray != null && entityColumns != null) {
            EntityColumn<E>[] includingRowStyleColumns = new EntityColumn[entityColumns.length + 1];
            includingRowStyleColumns[0] = createStyleEntityColumn(rowStylesExpressionArray);
            System.arraycopy(entityColumns, 0, includingRowStyleColumns, 1, entityColumns.length);
            setEntityColumnsPrivate(includingRowStyleColumns);
        }
    }

    private void collectColumnsPersistentFields() {
        if (entityColumns == null)
            columnsPersistentFields = null;
        else {
            List<Expression<E>> columnsPersistentTerms = new ArrayList<>();
            ReactiveDqlQuery<E> reactiveDqlQuery = reactiveEntitiesMapper.getReactiveDqlQuery();
            reactiveDqlQuery.executeParsingCode(() -> {
                DomainModel domainModel = reactiveDqlQuery.getDomainModel();
                Object domainClassId = reactiveDqlQuery.getDomainClassId();
                for (EntityColumn<E> entityColumn : entityColumns) {
                    entityColumn.parseExpressionDefinitionIfNecessary(domainModel, domainClassId);
                    entityColumn.getDisplayExpression().collectPersistentTerms(columnsPersistentTerms);
                }
            });
            columnsPersistentFields = new ExpressionArray<>(columnsPersistentTerms).toString();
        }
    }

    protected abstract EntityColumn<E> createStyleEntityColumn(ExpressionArray<E> rowStylesExpressionArray);

    private DomainClass getDomainClass() {
        return reactiveEntitiesMapper.getReactiveDqlQuery().getDomainClass();
    }

    protected abstract void onEntityListChanged(EntityList<E> entityList);
}
