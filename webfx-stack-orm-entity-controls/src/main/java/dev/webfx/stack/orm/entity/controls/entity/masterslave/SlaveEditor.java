package dev.webfx.stack.orm.entity.controls.entity.masterslave;

import dev.webfx.stack.orm.entity.Entity;

/**
 * @author Bruno Salmon
 */
public interface SlaveEditor<E extends Entity> {

    void showChangeApprovalDialog(Runnable onApprovalCallback);

    void setSlave(E approvedSlave);

    E getSlave();

    boolean hasChanges();
}
