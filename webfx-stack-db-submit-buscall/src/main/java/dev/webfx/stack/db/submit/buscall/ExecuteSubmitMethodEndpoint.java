package dev.webfx.stack.db.submit.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitMethodEndpoint extends AsyncFunctionBusCallEndpoint<SubmitArgument, SubmitResult> {

    public ExecuteSubmitMethodEndpoint() {
        super(SubmitMethodAddress.EXECUTE_SUBMIT_METHOD_ADDRESS, SubmitService::executeSubmit);
    }
}
