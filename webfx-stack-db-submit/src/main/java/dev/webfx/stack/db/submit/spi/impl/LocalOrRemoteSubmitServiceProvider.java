package dev.webfx.stack.db.submit.spi.impl;

import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public class LocalOrRemoteSubmitServiceProvider extends LocalSubmitServiceProvider {

    protected Future<SubmitResult> executeRemoteSubmit(SubmitArgument argument) {
        return BusCallService.call(SubmitService.SUBMIT_SERVICE_ADDRESS, argument);
    }

    protected Future<Batch<SubmitResult>> executeRemoteSubmitBatch(Batch<SubmitArgument> batch) {
        return BusCallService.call(SubmitService.SUBMIT_BATCH_SERVICE_ADDRESS, batch);
    }

}
