package dev.webfx.stack.db.submit;

import dev.webfx.stack.com.buscall.spi.AsyncFunctionBusCallEndpoint;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitBusCallEndpoint extends AsyncFunctionBusCallEndpoint<SubmitArgument, SubmitResult> {

    public ExecuteSubmitBusCallEndpoint() {
        super(SubmitService.SUBMIT_SERVICE_ADDRESS, SubmitService::executeSubmit);
    }
}
