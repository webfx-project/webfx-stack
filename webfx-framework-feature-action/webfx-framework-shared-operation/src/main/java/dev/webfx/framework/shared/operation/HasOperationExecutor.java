package dev.webfx.framework.shared.operation;

import dev.webfx.platform.shared.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface HasOperationExecutor<Rq, Rs> {

    AsyncFunction<Rq, Rs> getOperationExecutor();

}
