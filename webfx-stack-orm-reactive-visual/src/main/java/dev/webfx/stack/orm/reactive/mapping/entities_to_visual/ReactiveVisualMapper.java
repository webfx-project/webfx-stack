package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.visual.*;
import dev.webfx.kit.util.properties.FXProperties;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class ReactiveVisualMapper<E extends Entity> extends ReactiveGridMapper<E>
    implements ReactiveVisualMapperAPI<E, ReactiveVisualMapper<E>> {

    // Flags used to distinguish internal changes from external changes and prevent reentrant calls.
    private boolean syncingFromSelectedEntities;
    private boolean syncingFromSelectedEntity;
    private boolean syncingFromRequestedSelectedEntity;
    private boolean syncingFromVisualSelection;

    private boolean clearAutoSelectSingleRowOnNextResultSet;

    // An always up-to-date observable list that represents the selected entities. As this is a list, this is the object
    // to work with when the application code is dealing with multiple selections. But if it is dealing with mono
    // selection only, then it will be more convenient to work with selectedEntityProperty instead.
    // This list is synced bidirectionally with the visual selection. So when the user changes the visual selection,
    // this updates this list. But if this is the application code that changes this list (exposed as public), the
    // visual selection will be synced to reflect that new selection. Also, ReactiveVisualMapper will ensure that
    // `selectedEntities` is always a sublist of the loaded entities. If the application code tries to add entities that
    // are not from the loaded entities, they will automatically be removed from that observable list.
    private final ObservableList<E> selectedEntities = FXCollections.observableArrayList();

    // `selectedEntityProperty` is especially designed for mono selection and represents the selected entity (null if no
    // selection). It's actually mapped as the first element of selectedEntities. So it's also possible to use in with
    // multiple selections, knowing that it will be the first selected entities. selectedEntityProperty is synced
    // bidirectionally with selectedEntities. So if the application code changes `selectedEntityProperty`, this will
    // set selectedEntities (and consequently the visual selection), and if the application code changes selectedEntities
    // (or if the user changes the visual selection -> which changes selectedEntities), this will also set
    // selectedEntityProperty.
    private final ObjectProperty<E> selectedEntityProperty = FXProperties.newObjectProperty(this::onSelectedEntitiesChanged);

    private void onSelectedEntitiesChanged(E selectedEntity) {
        // System.out.println("selectedEntity = " + selectedEntity);
        // Preventing reentrant calls from internal operations
        if (syncingFromSelectedEntity)
            return;

        syncingFromSelectedEntity = true;
        // Managing the sync selectedEntityProperty -> selectedEntities
        if (!syncingFromSelectedEntities) { // already done if syncing from selectedEntities
            // As this change is coming from the application code - which preferred to work with selectedEntity rather
            // than selectedEntities - we need to update selectedEntities to this single selection.
            if (selectedEntity == null) // If it's null, we clear selectedEntities
                selectedEntities.clear();
            else // If it's not null, we apply it as a single element to selectedEntities
                selectedEntities.setAll(selectedEntity);
            // Note: this code above has triggered the selectedEntities listener that did the sync in the opposite
            // direction (`selectedEntities` -> `selectedEntityProperty`), but the reentrant call will be prevented.
            // However, it's possible that the value of selectedEntityProperty has changed if the entity asked by the
            // application was not valid (i.e., not present in the loaded entities). So we refresh the value.
            selectedEntity = getSelectedEntity();
        }

        // If the change doesn't come from requestedSelectedEntity but is a later change from the application
        // code or from the user selection, we do the back sync selectedEntity -> requestedSelectedEntity
        if (!syncingFromRequestedSelectedEntity)
            requestSelectedEntity(selectedEntity); // reentrant call will be prevented by syncingFromSelectedEntity

        // Finally, we call the selectedEntityHandler (if set)
        if (selectedEntityHandler != null) {
            //System.out.println("Calling selectedEntityHandler");
            selectedEntityHandler.accept(selectedEntity);
        }

        syncingFromSelectedEntity = false;
    }

    private final ObjectProperty<E> requestedSelectedEntityProperty = FXProperties.newObjectProperty(() -> {
        //System.out.println("requestedSelectedEntity = " + get());
        // Preventing reentrant calls from internal operations
        if (syncingFromSelectedEntity)
            return;
        // Syncing the selected entity with the requested selected entity (if appropriate at this time)
        syncFromRequestedSelectedEntity();
    });

    private void syncFromRequestedSelectedEntity() {
        // We don't do anything if the reactive visual mapper is not active (which happens when its associated activity
        // is not displayed at that time). If a MasterSlaveLinker is associated with this ReactiveVisualMapper,
        // we don't want it to display a confirmation dialog at this stage (if the slave entity being edited has changes).
        // The appropriate time to display the dialog is when this reactive visual mapper is active again (which happens
        // when the users go back to this activity).
        if (!isActive())
            return;
        // We attempt to apply the requested selected entity as the selected entity. If that entity is not part
        // of the loaded entities, this will be refused and the selected entity will be set to null instead. However,
        // `requestedSelectedEntityProperty` will not be set to null. This behavior is mainly to allow binding
        // `requestedSelectedEntityProperty` with another property that shouldn't be set back to null in that case.
        E requestedSelectedEntity = getRequestedSelectedEntity();
        if (!Objects.equals(getSelectedEntity(), requestedSelectedEntity)) { // not necessary to sync if already equals
            // We check if the requested selected entity is part of loaded entities
            if (requestedSelectedEntity != null) {
                int entityIndex = findEntityIndex(requestedSelectedEntity);
                // if no, we will set the selected entity to null
                if (entityIndex < 0)
                    requestedSelectedEntity = null;
                else // if yes, we get that loaded entity (which can be a different instance if `requestedSelectedEntity`
                    // comes from another entity store).
                    requestedSelectedEntity = getEntityAt(entityIndex);
            }
            // We apply the new selection
            syncingFromRequestedSelectedEntity = true;
            setSelectedEntity(requestedSelectedEntity);
            syncingFromRequestedSelectedEntity = false;
        }
    }

    private final ObjectProperty<VisualResult> visualResultProperty = FXProperties.newObjectProperty(visualResult -> {
        // When the whole visual result has changed, we need to update the selection. We try to keep the selection
        // unchanged, which happens when the selected entities are still in that new result. For example, when this
        // comes from a server push where just some fields have changed but the entity list remains the same.
        // Otherwise, we reduce the selected entities to those that are still present in the new result.
        if (autoSelectSingleRow && visualResult != null &&visualResult.getRowCount() == 1) {
            setVisualSelection(VisualSelection.createSingleRowSelection(0));
            clearAutoSelectSingleRowOnNextResultSet = true;
        } else if (clearAutoSelectSingleRowOnNextResultSet) {
            setVisualSelection(null);
        } else if (selectedEntities.isEmpty() && getRequestedSelectedEntity() != null) {
            syncFromRequestedSelectedEntity();
        } else {
            syncFromSelectedEntities();
        }
    });

    private final ObjectProperty<VisualSelection> visualSelectionProperty = FXProperties.newObjectProperty(this::syncFromVisualSelection);

    private void syncFromVisualSelection() {
        // Preventing reentrant calls from internal operations
        if (syncingFromVisualSelection || syncingFromSelectedEntities)
            return;

        syncingFromVisualSelection = true;
        // On visual selection change from the user, we need to update the selectedEntities to match that new visual
        // selection. So first, we transform that new visual selection into a fresh list of selected entities.
        List<E> newSelectedEntities = captureSelectedEntitiesFromVisualSelection();
        // And secondly, we apply that result into selectedEntities
        selectedEntities.setAll(newSelectedEntities); // This will eventually update `selectedEntityProperty` too
        syncingFromVisualSelection = false;

        clearAutoSelectSingleRowOnNextResultSet = false;
    }

    private E visualNullEntity;

    public ReactiveVisualMapper(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        super(reactiveEntitiesMapper);
        // Calling syncFromSelectedEntities() on selected entities changes
        selectedEntities.addListener((InvalidationListener) observable ->
            syncFromSelectedEntities());
        // Calling syncFromRequestedSelectedEntity() on active changes (to possibly show the confirmation dialog on activity resume)
        FXProperties.runOnPropertyChange(this::syncFromRequestedSelectedEntity, activeProperty());
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

    public E getRequestedSelectedEntity() {
        return requestedSelectedEntityProperty.get();
    }

    public ObjectProperty<E> requestedSelectedEntityProperty() {
        return requestedSelectedEntityProperty;
    }

    public ReactiveVisualMapper<E> requestSelectedEntity(E requestedSelectedEntity) {
        requestedSelectedEntityProperty.set(requestedSelectedEntity);
        return this;
    }

    // This is the code responsible for updating the visual selection when the application code changes the selected
    // entities. It also maintains the relationship between selectedEntities and selectedEntityProperty.
    private void syncFromSelectedEntities() {
        // Preventing reentrant calls.
        if (syncingFromSelectedEntities)
            return;

        syncingFromSelectedEntities = true;
        // First, because `selectedEntities` is meant to be a subset of the loaded entities only, we reduce the
        // list to the entities already loaded only (we remove those that are not found in the loaded entities).
        if (!syncingFromVisualSelection) {
            List<Integer> indexes = selectedEntities.stream().mapToInt(this::findEntityIndex).boxed().collect(Collectors.toList());
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
            setVisualSelection(VisualSelection.createRowsSelection(indexes));
        }

        // Finally, we update `selectedEntity`
        E selectedEntity = Collections.first(selectedEntities);
        // If the selected entity changed, we set the selectedEntityProperty and this will trigger onSelectedEntitiesChanged()
        if (selectedEntity != getSelectedEntity())
            setSelectedEntity(selectedEntity);
        // If it didn't change, we still want to call onSelectedEntitiesChanged(). Ex: If the user clicks on the same
        // entity in the EntityButtonSelector drop-down dialog, we still want to notify that selection event, so the
        // EntityButtonSelector closes the dialog when this happens.
        else if (syncingFromVisualSelection)
            onSelectedEntitiesChanged(selectedEntity);

        syncingFromSelectedEntities = false;
    }

    private int findEntityIndex(E entity) {
        EntityList<E> entities = getEntities();
        int index = entities == null ? -1 : entities.indexOf(entity); // Should work event if the entity comes from another store (see DynamicEntity.equals())
        return index;
    }

    @Override
    public ReactiveVisualMapper<E> getReactiveVisualMapper() {
        return this;
    }

    private VisualSelection getVisualSelection() {
        return visualSelectionProperty.get();
    }

    private void setVisualSelection(VisualSelection visualSelection) {
        visualSelectionProperty.set(visualSelection);
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

    @SafeVarargs
    @Override
    public final ReactiveVisualMapper<E> setEntityColumns(EntityColumn<E>... entityColumns) {
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
        // We will set `visualResultProperty` to the passed visual result, but if this property is bound to a VisualGrid,
        // this will also reset the visual selection, which we don't want in case a selected entity has been requested.
        // In this case, this ReactiveVisualMapper will indeed set the selection in visualResultProperty.onInvalidated()
        // and the VisualGrid would reset it immediately after if we don't ask it to prevent this default behavior.
        boolean oldPrevented = VisualSelection.isVisualSelectionResetPrevented();
        if (getRequestedSelectedEntity() != null) // we prevent the selection reset in this case
            VisualSelection.setVisualSelectionResetPrevented(true);
        // Now we are good to reset `visualResultProperty`
        visualResultProperty.set(rs);
        // We re-establish the previous value
        VisualSelection.setVisualSelectionResetPrevented(oldPrevented);
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
