package dev.webfx.stack.db.submit;

import dev.webfx.stack.com.buscall.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.async.Batch;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitBatchBusCallEndpoint extends AsyncFunctionBusCallEndpoint<Batch<SubmitArgument>, Batch<SubmitResult>> {

    public ExecuteSubmitBatchBusCallEndpoint() {
        super(SubmitService.SUBMIT_BATCH_SERVICE_ADDRESS, SubmitService::executeSubmitBatch);
    }
}
