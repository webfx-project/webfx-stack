package dev.webfx.stack.ui.operation;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public final class OperationUtil {

    public static <Rq, Rs> Future<Rs> executeOperation(Rq operationRequest, AsyncFunction<Rq, Rs> operationExecutor) {
        if (operationExecutor == null && operationRequest instanceof HasOperationExecutor)
            operationExecutor = ((HasOperationExecutor) operationRequest).getOperationExecutor();
        if (operationExecutor != null)
            return operationExecutor.apply(operationRequest);
        return Future.failedFuture(new IllegalArgumentException("No executor found for operation request " + operationRequest));
    }


}
