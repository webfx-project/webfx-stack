package dev.webfx.stack.orm.entity.controls.entity.masterslave;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public interface SlaveEditor<T> {

    void showSlaveSwitchApprovalDialog(Consumer<Boolean> approvalCallback);

    void setSlave(T approvedSlave);

    T getSlave();

    boolean hasChanges();
}
