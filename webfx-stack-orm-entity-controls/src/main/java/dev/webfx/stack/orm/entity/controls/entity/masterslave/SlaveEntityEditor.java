package dev.webfx.stack.orm.entity.controls.entity.masterslave;

import dev.webfx.stack.orm.entity.Entity;

/**
 * @author Bruno Salmon
 */
public interface SlaveEntityEditor<E extends Entity> {

    public void showEntityChangeApprovalDialog(Runnable onApprovalCallback);

    public void setEditingEntity(E approvedEntity);

    public E getEditingEntity();

    public boolean hasChanges();
}
