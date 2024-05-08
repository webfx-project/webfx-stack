package dev.webfx.stack.orm.entity.controls.entity.masterslave;

import dev.webfx.stack.orm.entity.Entity;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class MasterSlaveLinker<E extends Entity> {

    private boolean internalMasterChange;

    private final ObjectProperty<E> masterEntityProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            // System.out.println("masterEntity = " + get());
            // If it's coming from an internal change, we don't react
            if (internalMasterChange)
                return;
            // If the master and slave are already the same, we don't do anything
            if (Objects.equals(getMasterEntity(), getSlaveEntity())) // note: equals() returns true if same EntityId
                return;
            // Otherwise we check for approval if this master entity change can be applied to the slave editor
            checkSlaveEntityChangeApproval(false, () -> {
                // If it has been approved, we ask the slave editor to edit the master entity
                slaveEntityEditor.setSlave(getMasterEntity());
            });
        }
    };

    private final SlaveEditor<E> slaveEntityEditor;

    public MasterSlaveLinker(SlaveEditor<E> slaveEntityEditor) {
        this.slaveEntityEditor = slaveEntityEditor;
    }

    public E getMasterEntity() {
        return masterEntityProperty.get();
    }

    public ObjectProperty<E> masterEntityProperty() {
        return masterEntityProperty;
    }

    public void setMasterEntity(E masterEntity) {
        masterEntityProperty.set(masterEntity);
    }

    private E getSlaveEntity() {
        return slaveEntityEditor.getSlave();
    }

    public void checkSlaveEntityChangeApproval(boolean clearMasterEntityOnApproval, Runnable onApprovalCallback) {
        // Case of immediate approval
        if (getSlaveEntity() == null || !slaveEntityEditor.hasChanges())
            callOnApprovalCallback(clearMasterEntityOnApproval, onApprovalCallback);
        else { // Otherwise we need to ask the user for approval
            Platform.runLater(() -> { // The reason why we postpone the call to show the dialog is to ensure that the
                // dialog area (in Modality) matches the current activity. Otherwise, if this code is ran while resuming
                // the activity (which happens when masterEntityProperty is bound to the selectedEntityProperty of a
                // ReactiveVisualMapper which becomes active again on activity resume), the dialog area hasn't been
                // updated yet, and the dialog would be displayed in the leaving activity instead of the entering activity.
                slaveEntityEditor.showChangeApprovalDialog(() -> callOnApprovalCallback(clearMasterEntityOnApproval, onApprovalCallback));
            });
        }
    }

    private void callOnApprovalCallback(boolean clearMasterEntityOnApproval, Runnable onApprovalCallback) {
        if (clearMasterEntityOnApproval) {
            internalMasterChange = true;
            setMasterEntity(null);
            internalMasterChange = false;
        }
        onApprovalCallback.run();
    }
}
