package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.visual.*;
import dev.webfx.platform.util.collection.Collections;
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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class ReactiveVisualMapper<E extends Entity> extends ReactiveGridMapper<E>
    implements ReactiveVisualMapperAPI<E, ReactiveVisualMapper<E>> {

    // Flag used to distinguish internal changes from external changes, to prevent reentrant calls.
    private boolean syncingFromSelectedEntities;

    // An always up-to-date observable list that represents the selected entities. As this is a list, this is the object
    // to work with when the application code is dealing with multiple selection. But if it is dealing with mono
    // selection only, then it will be more convenient to work with selectedEntityProperty instead.
    // This list is synced in a bidrectional manner with the visual selection. So when the user changes the visual
    // selection, this updates this list. But if this is the application code that changes this list (exposed as public),
    // the visual selection will be synced to reflect that new selection. Also ReactiveVisualMapper will ensure that
    // selectedEntities is always a sublist of the loaded entities. If the application code tries to add entities that
    // are not from the loaded entities, they will be automatically be removed from that observable list.
    private final ObservableList<E> selectedEntities = FXCollections.observableArrayList();

    // selectedEntityProperty is especially designed for mono selection and represents the selected entity (null if no
    // selection). It's actually mapped as the first element of selectedEntities. So it's also possible to use in with
    // multiple selections, knowing that it will be the first selected entities. selectedEntityProperty is synced in a
    // bidirectional manner with selectedEntities. So if the application code changes selectedEntityProperty this will
    // set selectedEntities (and consequently the visual selection), and if the application code changes selectedEntities
    // (or if the user changes the visual selection -> which changes selectedEntities), this will also set
    // selectedEntityProperty.
    private final ObjectProperty<E> selectedEntityProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            //System.out.println("syncingFromSelectedEntities = " + syncingFromSelectedEntities + ", selectedEntity = " + getSelectedEntity());
            // Preventing reentrant calls from internal operations
            if (syncingFromSelectedEntities)
                return;

            // As this change is coming from the application code - which preferred to work with selectedEntity rather
            // than selectedEntities - we need to update selectedEntities to this single selection.
            E selectedEntity = getSelectedEntity(); // Getting the selected entity asked by the application code
            if (selectedEntity == null) // If it's null, we clear selectedEntities
                selectedEntities.clear();
            else // If it's not null, we apply it as a single element to selectedEntities
                selectedEntities.setAll(selectedEntity);
            // Note: this above code has triggered the selectedEntities listener that did the sync in the opposite
            // direction (selectedEntities -> selectedEntityProperty) but the reentrant call will has been prevented.
            // However, it's possible that the value of selectedEntityPropery has changed if the entity asked by the
            // application was not valid (ie. not present in the loaded entities).

            // Finally, we call the selectedEntityHandler (if set)
            if (selectedEntityHandler != null) {
                selectedEntity = getSelectedEntity(); // because the value may have changed (see comment above)
                //System.out.println("Calling selectedEntityHandler");
                selectedEntityHandler.accept(selectedEntity);
            }
        }
    };

    private final ObjectProperty<VisualResult> visualResultProperty = new SimpleObjectProperty<VisualResult/*GWT*/>() {
        @Override
        protected void invalidated() {
            // When the whole visual result has changed, we need to update the selection. We try to keep the selection
            // unchanged, which happens when the selected entities are still in that new result (for example when this
            // comes from a server push where just some fields have changed but the entity list remains the same).
            // Otherwise, we reduce the selected entities to those that are still present in the new result.
            syncFromSelectedEntities();

            if (autoSelectSingleRow && get() != null && get().getRowCount() == 1)
                visualSelectionProperty.setValue(VisualSelection.createSingleRowSelection(0));
        }
    };

    private final ObjectProperty<VisualSelection> visualSelectionProperty = new SimpleObjectProperty<VisualSelection/*GWT*/>() {
        @Override
        protected void invalidated() {
            // Preventing reentrant calls from internal operations
            if (syncingFromSelectedEntities)
                return;
            // On visual selection change from the user, we need to update the selectedEntities to match that new visual
            // selection. So firstly, we transform that new visual selection into a fresh list of selected entities.
            List<E> newSelectedEntities = captureSelectedEntitiesFromVisualSelection();
            // And secondly, we apply that result into selectedEntities
            selectedEntities.setAll(newSelectedEntities); // This will eventually update selectedEntityProperty too
        }
    };

    private E visualNullEntity;

    public ReactiveVisualMapper(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        super(reactiveEntitiesMapper);
        // This is the code responsible for updating the visual selection when the application code changes the selected
        // entities. It also maintains the relationship between selectedEntities and selectedEntityProperty.
        selectedEntities.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                syncFromSelectedEntities();
            }
        });
    }

    // Exposing selectedEntities and selectedEntityProperty in public methods

    @Override
    public ObservableList<E> getSelectedEntities() {
        return selectedEntities;
    }


    public ReactiveVisualMapper<E> setSelectedEntities(List<E> newSelectedEntities) {
        selectedEntities.setAll(newSelectedEntities);
        return this;
    }

    @Override
    public E getSelectedEntity() {
        return selectedEntityProperty.get();
    }

    public ReactiveVisualMapper<E> setSelectedEntity(E selectedEntity) {
        selectedEntityProperty.set(selectedEntity);
        return this;
    }

    public ObjectProperty<E> selectedEntityProperty() {
        return selectedEntityProperty;
    }

    private void syncFromSelectedEntities() {
        // Preventing reentrant calls.
        if (syncingFromSelectedEntities)
            return;
        syncingFromSelectedEntities = true;
        // First, because selectedEntities is meant to be a subset of the loaded entities only, we reduce the
        // list to the entities already loaded only (we remove those that are not found in the loaded entities).
        List<Integer> indexes = selectedEntities.stream().mapToInt(e -> findEntityIndex(e)).boxed().collect(Collectors.toList());
        List<E> absentEntities = null;
        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i) == -1) {
                if (absentEntities == null)
                    absentEntities = new ArrayList<>();
                absentEntities.add(selectedEntities.get(i));
                indexes.remove(i);
                i--;
            }
        }
        if (absentEntities != null) {
            selectedEntities.removeAll(absentEntities);
        }
        // Second, we update the visual selection
        visualSelectionProperty.set(VisualSelection.createRowsSelection(indexes));
        syncingFromSelectedEntities = false;
        // Finally, we update selectedEntity
        selectedEntityProperty.set(Collections.first(selectedEntities));
    }

    private int findEntityIndex(E entity) {
        EntityList<E> entities = getEntities();
        int index = entities.indexOf(entity); // Should work event if entity comes from another store (see DynamicEntity.equals())
        return index;
    }

    @Override
    public ReactiveVisualMapper<E> getReactiveVisualMapper() {
        return this;
    }

    private VisualSelection getVisualSelection() {
        return visualSelectionProperty.get();
    }

    private List<E> captureSelectedEntitiesFromVisualSelection() {
        VisualSelection selection = getVisualSelection();
        if (selection == null)
            return java.util.Collections.emptyList();
        List<E> selectedEntities = new ArrayList<>();
        selection.forEachRow(row -> selectedEntities.add(getEntityAt(row)));
        return selectedEntities;
    }

    private E getEntityAt(int row) {
        return getCurrentEntities().get(row);
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
        visualResultProperty.set(rs);
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
