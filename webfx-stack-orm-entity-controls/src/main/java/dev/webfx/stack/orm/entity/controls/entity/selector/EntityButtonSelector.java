package dev.webfx.stack.orm.entity.controls.entity.selector;

import dev.webfx.extras.cell.renderer.ValueRenderer;
import dev.webfx.extras.cell.renderer.ValueRendererFactory;
import dev.webfx.extras.controlfactory.button.ButtonFactoryMixin;
import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.visual.VisualResult;
import dev.webfx.extras.visual.controls.grid.VisualGrid;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.function.Callable;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.dql.DqlStatement;
import dev.webfx.stack.orm.dql.DqlStatementBuilder;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.Parameter;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumn;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.ReactiveVisualMapper;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.ReactiveVisualMapperAPI;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.VisualEntityColumnFactory;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static dev.webfx.stack.orm.dql.DqlStatement.limit;
import static dev.webfx.stack.orm.dql.DqlStatement.where;

/**
 * @author Bruno Salmon
 */
public class EntityButtonSelector<E extends Entity> extends ButtonSelector<E> implements ReactiveVisualMapperAPI<E, EntityButtonSelector<E>> {

    private final ObjectProperty<VisualResult> deferredVisualResult = new SimpleObjectProperty<>();
    private Object jsonOrClass;
    private final DataSourceModel dataSourceModel;
    private DomainClass entityClass; // the domain class found from jsonOrClass & dataSourceModel
    private Expression<E> renderingExpression;
    private ValueRenderer entityRenderer;

    private EntityStore loadingStore;
    private ReactiveVisualMapper<E> entityDialogMapper;
    private List<E> restrictedFilterList;
    private VisualGrid dialogVisualGrid;
    private ScalePane scalePane;

    private String searchCondition;
    // Named parameters within the search condition (extracted after expression parsing)
    private Parameter[] searchConditionNamedParameters; // Ex:

    // Good to put a limit, especially for low-end mobiles
    private int adaptiveLimit = 6; // starting with 6 entries (fit with drop down/up) but can be increased in modal in dependence of the available height

    public EntityButtonSelector(Object jsonOrClass, ButtonFactoryMixin buttonFactory, Callable<Pane> parentGetter, DataSourceModel dataSourceModel) {
        this(jsonOrClass, buttonFactory, parentGetter, null, dataSourceModel);
    }

    public EntityButtonSelector(Object jsonOrClass, ButtonFactoryMixin buttonFactory, Pane parent, DataSourceModel dataSourceModel) {
        this(jsonOrClass, buttonFactory, null, parent, dataSourceModel);
    }

    protected EntityButtonSelector(Object jsonOrClass, ButtonFactoryMixin buttonFactory, Callable<Pane> parentGetter, Pane parent, DataSourceModel dataSourceModel) {
        this(jsonOrClass, dataSourceModel, new ButtonSelectorParameters().setButtonFactory(buttonFactory).setDropParentGetter(parentGetter).setDropParent(parent));
    }

    protected EntityButtonSelector(Object jsonOrClass, DataSourceModel dataSourceModel, ButtonSelectorParameters buttonSelectorParameters) {
        super(buttonSelectorParameters);
        this.dataSourceModel = dataSourceModel;
        setJsonOrClass(jsonOrClass);
        setLoadedContentProperty(deferredVisualResult);
    }

    public List<E> getRestrictedFilterList() {
        return restrictedFilterList;
    }

    public EntityButtonSelector<E> setRestrictedFilterList(List<E> restrictedFilterList) {
        this.restrictedFilterList = restrictedFilterList;
        return this;
    }

    public EntityButtonSelector<E> setJsonOrClass(Object jsonOrClass) {
        this.jsonOrClass = jsonOrClass;
        renderingExpression = null;
        dialogVisualGrid = null;
        if (jsonOrClass != null) {
            DqlStatement dqlStatement = new DqlStatementBuilder(jsonOrClass).build();
            DomainModel domainModel = dataSourceModel.getDomainModel();
            Object domainClassId = dqlStatement.getDomainClassId();
            entityClass = domainModel.getClass(domainClassId);
            if (dqlStatement.getColumns() != null) {
                EntityColumn<E>[] entityColumns = VisualEntityColumnFactory.get().fromJsonArrayOrExpressionsDefinition(dqlStatement.getColumns(), entityClass);
                renderingExpression = new ExpressionArray<>(Arrays.map(entityColumns, expressionColumn -> expressionColumn.parseExpressionDefinitionIfNecessary(entityClass).getDisplayExpression(), Expression[]::new));
            } else
                renderingExpression = entityClass.getForeignFields();
            String fields = dqlStatement.getFields();
            if (renderingExpression == null && fields != null)
                renderingExpression = entityClass.parseExpression(fields);
            setSearchCondition(entityClass.getSearchCondition());
        }
        entityRenderer = renderingExpression == null ? null : ValueRendererFactory.getDefault().createValueRenderer(renderingExpression.getType());
        if (entityRenderer == null) {
            Console.log("⚠️ WARNING: EntityButtonSelector couldn't find any domain renderer! Please fix this issue by specifying fields or columns in : " + jsonOrClass);
        }
        forceDialogRebuiltOnNextShow();
        return this;
    }

    public EntityButtonSelector<E> setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
        searchConditionNamedParameters = null;
        return this;
    }

    private Parameter[] getSearchConditionNamedParameters() {
        if (searchConditionNamedParameters == null) {
            // We parse the search condition and collect the terms including the parameters
            CollectOptions collectOptions = new CollectOptions().setIncludeParameter(true).setTraverseSqlExpressible(true);
            entityClass.parseExpression(searchCondition).collect(collectOptions);
            // Then we filter the parameters from the collected terms and put them in an array
            searchConditionNamedParameters = collectOptions.getCollectedTerms().stream().filter(e -> e instanceof Parameter).map(e -> (Parameter) e).toArray(Parameter[]::new);
        }
        return searchConditionNamedParameters;
    }

    public EntityButtonSelector<E> setLoadingStore(EntityStore loadingStore) {
        this.loadingStore = loadingStore;
        return this;
    }

    @Override
    protected Node getOrCreateButtonContentFromSelectedItem() {
        E entity = getSelectedItem();
        if (entity == null)
            entity = getVisualNullEntity();
        Object renderedValue = entity == null ? null : entity.evaluate(renderingExpression);
        return entityRenderer.renderValue(renderedValue);
    }

    @Override
    protected Region getOrCreateDialogContent() {
        if (dialogVisualGrid == null && entityRenderer != null) {
            dialogVisualGrid = VisualGrid.createVisualGridWithTableLayoutSkin();
            dialogVisualGrid.setHeaderVisible(false);
            dialogVisualGrid.setCursor(Cursor.HAND);
            BorderPane.setAlignment(dialogVisualGrid, Pos.TOP_LEFT);
            FXProperties.runOnPropertyChange(visualResult -> Platform.runLater(() -> deferredVisualResult.setValue(visualResult))
                , dialogVisualGrid.visualResultProperty());
            EntityStore filterStore = loadingStore != null ? loadingStore : getSelectedItem() != null ? getSelectedItem().getStore() : null;
            entityDialogMapper = ReactiveVisualMapper.<E>createReactiveChain()
                .always(jsonOrClass)
                .setDataSourceModel(dataSourceModel)
                .setStore(filterStore)
                .setRestrictedFilterList(restrictedFilterList)
                .setEntityColumns(VisualEntityColumnFactory.get().create(renderingExpression))
                .visualizeResultInto(dialogVisualGrid)
                .setSelectedEntityHandler(e -> {
                    if (/*e != null && */button != null && !Entities.sameId(e, getSelectedItem()))
                        onDialogOk();
                });
            if (isSearchEnabled())
                entityDialogMapper
                    .ifTrimNotEmpty(searchTextProperty(), s -> {
                        DqlStatement[] where = { null };
                        // We embed the code with executeParsingCode() in order to resolve a possible reference in
                        // searchCondition to the alias - if set (ex: alias: p and "searchMatchesPerson(p)")
                        executeParsingCode(() -> {
                            EntityStore store = entityDialogMapper.getReactiveEntitiesMapper().getStore();
                            setSearchParameters(s, store);
                            where[0] = where(searchCondition, java.util.Arrays.stream(getSearchConditionNamedParameters()).map(e -> e.evaluate(null, store.getEntityDataWriter())).toArray());
                        });
                        return where[0];
                    })
                    .always(dialogHeightProperty(), height -> limit("?", updateAdaptiveLimit(height)));
            //dialogDataGrid.setOnMouseClicked(e -> {if (e.isPrimaryButtonDown() && e.getClickCount() == 1) onDialogOk(); });
            // Embedding the visual grid in a ScalePane that can only grow (up to 3x times) to avoid small rows on big screens
            scalePane = new ScalePane(dialogVisualGrid);
            scalePane.setCanShrink(false);
            scalePane.setMaxScale(3); // Should this value be parameterized?
            scalePane.setScaleRegion(true);  // Otherwise stretch the region without scaling it
            scalePane.setStretchWidth(true); // Actually shrinks the grid width back to fit again in the dialog
            scalePane.setVAlignment(VPos.TOP); // We want the scaled grid to be aligned on top
            scalePane.setScaleMode(ScaleMode.FIT_WIDTH); // The scale depends on the dialog width
            // Setting a quite arbitrary pref width value (otherwise the scale will vary depending on the data displayed)
            dialogVisualGrid.setPrefWidth(300); // Should this value be parameterized?
            // Now that scalePane is set up, we set up the searchPane (also a ScalePane) to give it the same scale.
            searchPane.setStretchWidth(true); // Actually shrinks the grid width back to fit again in the dialog
            searchPane.setScaleMode(ScaleMode.FIT_HEIGHT); // We will manually stretch the height to control the scale
            // We multiply the height by the same scale factor as the one applied on the visual grid to get the same scale
            FXProperties.runOnDoublePropertyChange(visualGridScaleY -> {
                // First, we compute the searchPane normal height (with no scale).
                searchPane.setPrefHeight(Region.USE_COMPUTED_SIZE); // Necessary to force the computation
                double prefHeight = searchPane.prefHeight(searchPane.getWidth());
                // Now we stretch the searchPane height with the visual grid scale factor
                searchPane.setPrefHeight(prefHeight * visualGridScaleY); // will scale the content (search text field and icon)
            }, dialogVisualGrid.scaleYProperty());
        }
        return scalePane;
    }

    private int updateAdaptiveLimit(Number height) {
        int maxNumberOfVisibleEntries = height.intValue() / 28;
        if (maxNumberOfVisibleEntries > adaptiveLimit)
            adaptiveLimit = maxNumberOfVisibleEntries + (getDecidedShowMode() == ShowMode.MODAL_DIALOG ? 6 : 0); // extra 6 to avoid repetitive requests when resizing the window
        return adaptiveLimit;
    }

    @Override
    public boolean isSearchEnabled() {
        return super.isSearchEnabled() && searchCondition != null;
    }

    protected void setSearchParameters(String search, EntityStore store) {
        store.setParameterValue("search", search);
        store.setParameterValue("lowerSearch", search.toLowerCase());
        store.setParameterValue("searchLike", "%" + search + "%");
        store.setParameterValue("lowerSearchLike", "%" + search.toLowerCase() + "%");
    }

    public EntityButtonSelector<E> autoSelectFirstEntity() {
        return autoSelectFirstEntity(e -> true);
    }

    public EntityButtonSelector<E> autoSelectFirstEntity(Predicate<E> predicate) {
        if (predicate == null)
            return this;
        setUpDialog(false);
        if (entityDialogMapper != null) {
            ReactiveEntitiesMapper<E> reactiveEntitiesMapper = entityDialogMapper.getReactiveEntitiesMapper();
            Consumer<EntityList<E>>[] entitiesHandlerHolder = new Consumer[1];
            reactiveEntitiesMapper.addEntitiesHandler(entitiesHandlerHolder[0] = entityList -> {
                setSelectedItem(entityList.stream().filter(predicate).findFirst().orElse(null));
                reactiveEntitiesMapper.removeEntitiesHandler(entitiesHandlerHolder[0]);
            });
        }
        return this;
    }

    public ReactiveVisualMapper<E> getReactiveVisualMapper() {
        if (entityDialogMapper == null) {
            getOrCreateDialogContent();
        }
        return entityDialogMapper;
    }

    @Override
    protected void setUpDialog(boolean show) {
        super.setUpDialog(show);
        getReactiveVisualMapper().start();
    }

    @Override
    protected void startLoading() {
        if (!getReactiveVisualMapper().isStarted())
            entityDialogMapper.start();
    }

    @Override
    protected void onDialogOk() {
        ReactiveVisualMapper<E> reactiveVisualMapper = getReactiveVisualMapper();
        E selectedEntity = reactiveVisualMapper.getSelectedEntity();
        if (selectedEntity == null && reactiveVisualMapper.getEntities().size() == 1)
            selectedEntity = reactiveVisualMapper.getEntities().get(0);
        boolean nullAllowed = reactiveVisualMapper.isNullEntityAppended();
        if (selectedEntity != null || nullAllowed)
            setSelectedItem(selectedEntity);
        super.onDialogOk();
    }

    @Override
    protected void closeDialog() {
        getReactiveVisualMapper().stop();
        super.closeDialog();
    }

    // Bumping Fluent API

    @Override
    public EntityButtonSelector<E> setAutoOpenOnMouseEntered(boolean autoOpenOnMouseEntered) {
        return (EntityButtonSelector<E>) super.setAutoOpenOnMouseEntered(autoOpenOnMouseEntered);
    }

    @Override
    public EntityButtonSelector<E> setSearchEnabled(boolean searchEnabled) {
        return (EntityButtonSelector<E>) super.setSearchEnabled(searchEnabled);
    }

    @Override
    public EntityButtonSelector<E> setSelectedItem(E item) {
        return (EntityButtonSelector<E>) super.setSelectedItem(item);
    }

    @Override
    public EntityButtonSelector<E> setReadOnly(boolean readOnly) {
        return (EntityButtonSelector<E>) super.setReadOnly(readOnly);
    }

    @Override
    public EntityButtonSelector<E> setButton(Button button) {
        return (EntityButtonSelector<E>) super.setButton(button);
    }

    @Override
    public EntityButtonSelector<E> setShowMode(ShowMode showModeProperty) {
        return (EntityButtonSelector<E>) super.setShowMode(showModeProperty);
    }

    @Override
    public EntityButtonSelector<E> setCloseHandler(Runnable closeHandler) {
        return (EntityButtonSelector<E>) super.setCloseHandler(closeHandler);
    }

    // For retro-compatibility TODO: remove once unused anymore
    @Deprecated
    public ReactiveVisualMapper<E> getEntityDialogMapper() {
        return getReactiveVisualMapper();
    }

}
