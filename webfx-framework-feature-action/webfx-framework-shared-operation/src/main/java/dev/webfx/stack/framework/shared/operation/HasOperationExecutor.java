package dev.webfx.stack.framework.shared.operation;

import dev.webfx.stack.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface HasOperationExecutor<Rq, Rs> {

    AsyncFunction<Rq, Rs> getOperationExecutor();

}
