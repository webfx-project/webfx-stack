package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.visual.*;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.reactive.dql.statement.conventions.HasSelectedGroupProperty;
import dev.webfx.stack.orm.reactive.dql.statement.conventions.HasSelectedGroupReferenceResolver;
import dev.webfx.stack.orm.reactive.dql.statement.conventions.HasSelectedMasterProperty;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumn;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.ReactiveGridMapper;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.conventions.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class ReactiveVisualMapper<E extends Entity> extends ReactiveGridMapper<E>
    implements ReactiveVisualMapperAPI<E, ReactiveVisualMapper<E>> {

    private final ObjectProperty<VisualResult> visualResultProperty = new SimpleObjectProperty<VisualResult/*GWT*/>() {
        @Override
        protected void invalidated() {
            if (autoSelectSingleRow && get() != null && get().getRowCount() == 1)
                visualSelectionProperty.setValue(VisualSelection.createSingleRowSelection(0));
        }
    };

    private final ObjectProperty<VisualSelection> visualSelectionProperty = new SimpleObjectProperty<VisualSelection/*GWT*/>() {
        @Override
        protected void invalidated() {
            VisualSelection visualSelection = get();
            if (selectedEntityHandler != null && VisualSelection.isEmptyOrSingleSelection(visualSelection))
                selectedEntityHandler.accept(getSelectedEntity());
        }
    };

    private E visualNullEntity;

    public ReactiveVisualMapper(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        super(reactiveEntitiesMapper);
    }

    @Override
    public ReactiveVisualMapper<E> getReactiveVisualMapper() {
        return this;
    }

    @Override
    public List<E> getSelectedEntities() {
        return getSelectedEntities(visualSelectionProperty.get());
    }

    private List<E> getSelectedEntities(VisualSelection selection) {
        if (selection == null)
            return null;
        List<E> selectedEntities = new ArrayList<>();
        selection.forEachRow(row -> selectedEntities.add(getEntityAt(row)));
        return selectedEntities;
    }

    @Override
    public E getSelectedEntity() {
        VisualSelection visualSelection = visualSelectionProperty.get();
        return visualSelection == null || visualSelection.isEmpty() ? null : getEntityAt(visualSelection.getSelectedRow());
    }

    private E getEntityAt(int row) {
        return getCurrentEntities().get(row);
    }

    public ReactiveVisualMapper<E> setSelectedEntity(E selectedEntity) {
        int row = -1;
        if (selectedEntity != null) {
            EntityList<E> currentEntities = getCurrentEntities();
            row = currentEntities.indexOf(selectedEntity); // works if they are both from the same store
            if (row == -1) { // otherwise, we need to compare the ids
                for (int i = 0; i < currentEntities.size(); i++) {
                    if (Entities.sameId(selectedEntity, currentEntities.get(i))) {
                        row = i;
                        break;
                    }
                }
            }
        }
        // We set the selection to null if row == -1, or to row otherwise, but we prevent firing a selection event
        // if this is not a change compared to the current selection
        VisualSelection visualSelection = visualSelectionProperty.get();
        if (row == -1) {
            if (visualSelection != null)
                visualSelectionProperty.set(null); // will fire selection event
        } else if (visualSelection == null || row != visualSelection.getSelectedRow()) {
             visualSelectionProperty.set(VisualSelection.createSingleRowSelection(row)); // will fire selection event
        }
        return this;
    }

    @Override
    public Property<VisualResult> visualResultProperty() {
        return visualResultProperty;
    }

    @Override
    public ReactiveVisualMapper<E> visualizeResultInto(HasVisualResultProperty hasVisualResultProperty) {
        if (hasVisualResultProperty instanceof HasVisualSelectionProperty)
            setVisualSelectionProperty(((HasVisualSelectionProperty) hasVisualResultProperty).visualSelectionProperty());
        return visualizeResultInto(hasVisualResultProperty.visualResultProperty());
    }

    @Override
    public ReactiveVisualMapper<E> visualizeResultInto(Property<VisualResult> visualResultProperty) {
        visualResultProperty.bind(this.visualResultProperty);
        return this;
    }

    @Override
    public ReactiveVisualMapper<E> setVisualSelectionProperty(Property<VisualSelection> visualSelectionProperty) {
        visualSelectionProperty.bindBidirectional(this.visualSelectionProperty);
        return this;
    }

    @Override
    public ReactiveVisualMapper<E> setVisualNullEntity(E visualNullEntity) {
        this.visualNullEntity = visualNullEntity;
        return this;
    }

    @Override
    public E getVisualNullEntity() {
        return visualNullEntity;
    }

    @Override
    public ReactiveVisualMapper<E> applyDomainModelRowStyle() {
        return (ReactiveVisualMapper<E>) super.applyDomainModelRowStyle();
    }

    @Override
    public ReactiveVisualMapper<E> autoSelectSingleRow() {
        return (ReactiveVisualMapper<E>) super.autoSelectSingleRow();
    }

    @Override
    public ReactiveVisualMapper<E> setSelectedEntityHandler(Consumer<E> selectedEntityHandler) {
        return (ReactiveVisualMapper<E>) super.setSelectedEntityHandler(selectedEntityHandler);
    }

    @Override
    public ReactiveVisualMapper<E> setEntityColumns(String jsonArrayOrExpressionDefinition) {
        return (ReactiveVisualMapper<E>) super.setEntityColumns(jsonArrayOrExpressionDefinition);
    }

    @Override
    public ReactiveVisualMapper<E> setEntityColumns(EntityColumn<E>... entityColumns) {
        return (ReactiveVisualMapper<E>) super.setEntityColumns(entityColumns);
    }

    @Override
    protected VisualEntityColumnFactory getEntityColumnFactory() {
        return VisualEntityColumnFactory.get();
    }

    @Override
    protected EntityColumn<E> createStyleEntityColumn(ExpressionArray<E> rowStylesExpressionArray) {
        return getEntityColumnFactory().create(rowStylesExpressionArray, VisualColumnBuilder.create("style", PrimType.STRING).setRole("style").build());
    }

    @Override
    protected void onEntityListChanged(EntityList<E> entityList) {
        setVisualResult(entitiesToVisualResult(entityList));
    }

    void setVisualResult(VisualResult rs) {
        List<E> previousSelection = getSelectedEntities();
        //System.out.println("ReactiveVisualMapper.setVisualResult()"); // + " result = " + rs);
        visualResultProperty.setValue(rs);
        if (autoSelectSingleRow && rs.getRowCount() == 1 || selectFirstRowOnFirstDisplay && rs.getRowCount() > 0) {
            selectFirstRowOnFirstDisplay = false;
            visualSelectionProperty.setValue(VisualSelection.createSingleRowSelection(0));
        } else if (previousSelection != null && previousSelection.size() == 1) {
            setSelectedEntity(previousSelection.get(0));
        }
    }

    VisualResult entitiesToVisualResult(List<E> entities) {
        return EntitiesToVisualResultMapper.mapEntitiesToVisualResult(entities, entityColumns, visualNullEntity);
    }

    /*==================================================================================================================
      ======================================= Classic static factory API ===============================================
      ================================================================================================================*/

    public static <E extends Entity> ReactiveVisualMapper<E> create(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        return new ReactiveVisualMapper<>(reactiveEntitiesMapper);
    }

    /*==================================================================================================================
      ==================================== Conventional static factory API =============================================
      ================================================================================================================*/

    public static <E extends Entity> ReactiveVisualMapper<E> createMaster(Object pm, ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        return initializeMaster(create(reactiveEntitiesMapper), pm);
    }

    protected static <E extends Entity> ReactiveVisualMapper<E> initializeMaster(ReactiveVisualMapper<E> master, Object pm) {
        if (pm instanceof HasMasterVisualResultProperty)
            master.visualizeResultInto(((HasMasterVisualResultProperty) pm).masterVisualResultProperty());
        if (pm instanceof HasMasterVisualSelectionProperty)
            master.setVisualSelectionProperty(((HasMasterVisualSelectionProperty) pm).masterVisualSelectionProperty());
        if (pm instanceof HasSelectedMasterProperty)
            master.setSelectedEntityHandler(((HasSelectedMasterProperty<E>) pm)::setSelectedMaster);
        return master;
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createGroup(Object pm, ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        return initializeGroup(create(reactiveEntitiesMapper), pm);
    }

    protected static <E extends Entity> ReactiveVisualMapper<E> initializeGroup(ReactiveVisualMapper<E> group, Object pm) {
        if (pm instanceof HasGroupVisualResultProperty)
            group.visualizeResultInto(((HasGroupVisualResultProperty) pm).groupVisualResultProperty());
        if (pm instanceof HasGroupVisualSelectionProperty)
            group.setVisualSelectionProperty(((HasGroupVisualSelectionProperty) pm).groupVisualSelectionProperty());
        if (pm instanceof HasSelectedGroupProperty)
            group.setSelectedEntityHandler(((HasSelectedGroupProperty) pm)::setSelectedGroup);
        if (pm instanceof HasSelectedGroupReferenceResolver)
            ((HasSelectedGroupReferenceResolver) pm).setSelectedGroupReferenceResolver(group.getReactiveEntitiesMapper().getReactiveDqlQuery().getRootAliasReferenceResolver());
        return group;
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createSlave(Object pm, ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        return initializeSlave(create(reactiveEntitiesMapper), pm);
    }

    protected static <E extends Entity> ReactiveVisualMapper<E> initializeSlave(ReactiveVisualMapper<E> slave, Object pm) {
        if (pm instanceof HasSlaveVisualResultProperty)
            slave.visualizeResultInto(((HasSlaveVisualResultProperty) pm).slaveVisualResultProperty());
        return slave;
    }

    /*==================================================================================================================
      ===================================== Shortcut static factory API ================================================
      ================================================================================================================*/

    public static <E extends Entity> ReactiveVisualMapper<E> createReactiveChain() {
        return create(ReactiveEntitiesMapper.createReactiveChain());
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createReactiveChain(Object mixin) {
        return create(ReactiveEntitiesMapper.createReactiveChain(mixin));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createPushReactiveChain() {
        return create(ReactiveEntitiesMapper.createPushReactiveChain());
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createPushReactiveChain(Object mixin) {
        return create(ReactiveEntitiesMapper.createPushReactiveChain(mixin));
    }

    // Master
    
    public static <E extends Entity> ReactiveVisualMapper<E> createMasterReactiveChain(Object pm) {
        return createMaster(pm, ReactiveEntitiesMapper.createMasterReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createMasterReactiveChain(Object mixin, Object pm) {
        return createMaster(pm, ReactiveEntitiesMapper.createMasterReactiveChain(mixin, pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createMasterPushReactiveChain(Object pm) {
        return createMaster(pm, ReactiveEntitiesMapper.createMasterPushReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createMasterPushReactiveChain(Object mixin, Object pm) {
        return createMaster(pm, ReactiveEntitiesMapper.createMasterPushReactiveChain(mixin, pm));
    }

    // Group
    
    public static <E extends Entity> ReactiveVisualMapper<E> createGroupReactiveChain(Object pm) {
        return createGroup(pm, ReactiveEntitiesMapper.createGroupReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createGroupReactiveChain(Object mixin, Object pm) {
        return createGroup(pm, ReactiveEntitiesMapper.createGroupReactiveChain(mixin, pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createGroupPushReactiveChain(Object pm) {
        return createGroup(pm, ReactiveEntitiesMapper.createGroupPushReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createGroupPushReactiveChain(Object mixin, Object pm) {
        return createGroup(pm, ReactiveEntitiesMapper.createGroupPushReactiveChain(mixin, pm));
    }

    // Slave

    public static <E extends Entity> ReactiveVisualMapper<E> createSlaveReactiveChain(Object pm) {
        return createSlave(pm, ReactiveEntitiesMapper.createSlaveReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createSlaveReactiveChain(Object mixin, Object pm) {
        return createSlave(pm, ReactiveEntitiesMapper.createSlaveReactiveChain(mixin, pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createSlavePushReactiveChain(Object pm) {
        return createSlave(pm, ReactiveEntitiesMapper.createSlavePushReactiveChain(pm));
    }

    public static <E extends Entity> ReactiveVisualMapper<E> createSlavePushReactiveChain(Object mixin, Object pm) {
        return createSlave(pm, ReactiveEntitiesMapper.createSlavePushReactiveChain(mixin, pm));
    }

}
