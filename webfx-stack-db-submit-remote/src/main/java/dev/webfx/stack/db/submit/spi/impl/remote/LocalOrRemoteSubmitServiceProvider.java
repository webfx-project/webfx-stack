package dev.webfx.stack.db.submit.spi.impl.remote;

import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.db.submit.buscall.SubmitMethodAddress;
import dev.webfx.stack.db.submit.spi.impl.LocalSubmitServiceProvider;

/**
 * @author Bruno Salmon
 */
public class LocalOrRemoteSubmitServiceProvider extends LocalSubmitServiceProvider {

    protected Future<SubmitResult> executeRemoteSubmit(SubmitArgument argument) {
        return BusCallService.call(SubmitMethodAddress.EXECUTE_SUBMIT_METHOD_ADDRESS, argument);
    }

    protected Future<Batch<SubmitResult>> executeRemoteSubmitBatch(Batch<SubmitArgument> batch) {
        return BusCallService.call(SubmitMethodAddress.EXECUTE_SUBMIT_BATCH_METHOD_ADDRESS, batch);
    }

}
