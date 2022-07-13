package dev.webfx.stack.orm.reactive.dql.statement.conventions;

public interface HasSlaveVisibilityCondition<E> {

    boolean isSlaveVisible(E selectedMaster);

}
