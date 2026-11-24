package dev.webfx.stack.db.submit.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.submit.ClientSubmitGuard;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

/**
 * @author Bruno Salmon
 */
public final class ExecuteSubmitMethodEndpoint extends AsyncFunctionBusCallEndpoint<SubmitArgument, SubmitResult> {

    public ExecuteSubmitMethodEndpoint() {
        // Every write arriving here was sent by a client, so it passes the client guard before it runs — see
        // ClientSubmitGuard for what that refuses, and why server code never comes through here.
        // The guard may answer later (a row rule that looks a row up), and by then this thread's state is gone — so
        // it is captured here and the write runs in it, as it would have had the guard answered at once.
        super(SubmitMethodAddress.EXECUTE_SUBMIT_METHOD_ADDRESS, argument -> {
            Object callerState = ThreadLocalStateHolder.getThreadLocalState();
            return ClientSubmitGuard.check(argument).compose(ignored ->
                ThreadLocalStateHolder.runWithState(callerState, () -> SubmitService.executeSubmit(argument)));
        });
    }
}
