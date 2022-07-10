package dev.webfx.stack.platform.shared.services.submit;

import dev.webfx.stack.platform.shared.services.buscall.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.platform.async.Batch;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitBatchBusCallEndpoint extends AsyncFunctionBusCallEndpoint<Batch<SubmitArgument>, Batch<SubmitResult>> {

    public ExecuteSubmitBatchBusCallEndpoint() {
        super(SubmitService.SUBMIT_BATCH_SERVICE_ADDRESS, SubmitService::executeSubmitBatch);
    }
}
