package dev.webfx.stack.db.submit.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.platform.async.Batch;
import dev.webfx.stack.db.submit.ClientSubmitGuard;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitBatchMethodEndpoint extends AsyncFunctionBusCallEndpoint<Batch<SubmitArgument>, Batch<SubmitResult>> {

    public ExecuteSubmitBatchMethodEndpoint() {
        // Every statement of a client's batch passes the client guard before any of it runs — see
        // ClientSubmitGuard.checkBatch.
        // Run in the caller's state even if the guard answered later — see ExecuteSubmitMethodEndpoint.
        super(SubmitMethodAddress.EXECUTE_SUBMIT_BATCH_METHOD_ADDRESS, batch -> {
            Object callerState = ThreadLocalStateHolder.getThreadLocalState();
            return ClientSubmitGuard.checkBatch(batch).compose(ignored ->
                ThreadLocalStateHolder.runWithState(callerState, () -> SubmitService.executeSubmitBatch(batch)));
        });
    }
}
