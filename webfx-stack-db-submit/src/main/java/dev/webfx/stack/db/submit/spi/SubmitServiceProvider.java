package dev.webfx.stack.db.submit.spi;

import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface SubmitServiceProvider {

    Future<SubmitResult> executeSubmit(SubmitArgument argument);

    default Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        return batch.executeSerial(SubmitResult[]::new, this::executeSubmit);
    }

}
