package dev.webfx.stack.ui.operation;

import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface HasOperationExecutor<Rq, Rs> {

    AsyncFunction<Rq, Rs> getOperationExecutor();

}
