package dev.webfx.stack.orm.entity.controls.entity.masterslave;

import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public class MasterSlaveLinker<E extends Entity> {

    private boolean internalMasterChange;

    private final ObjectProperty<E> masterEntityProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            // If it's coming from an internal change, we don't react
            if (internalMasterChange)
                return;
            // If the master and slave are already the same, we don't do anything
            if (Entities.sameId(getMasterEntity(), getSlaveEntity()))
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

    public void checkSlaveEntityChangeApproval(boolean clearMasterEntityOnAproval, Runnable onApprovalCallback) {
        // Case of immediate approval
        if (getSlaveEntity() == null || !slaveEntityEditor.hasChanges())
            callOnApprovalCallback(clearMasterEntityOnAproval, onApprovalCallback);
        else {
            // Otherwise we need to ask the user for approval
            slaveEntityEditor.showChangeApprovalDialog(() -> callOnApprovalCallback(clearMasterEntityOnAproval, onApprovalCallback));
        }
    }

    private void callOnApprovalCallback(boolean clearMasterEntityOnAproval, Runnable onApprovalCallback) {
        if (clearMasterEntityOnAproval) {
            internalMasterChange = true;
            setMasterEntity(null);
            internalMasterChange = false;
        }
        onApprovalCallback.run();
    }
}
