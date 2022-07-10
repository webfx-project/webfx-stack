package dev.webfx.stack.platform.shared.services.submit.spi;

import dev.webfx.stack.platform.shared.services.submit.SubmitArgument;
import dev.webfx.stack.platform.shared.services.submit.SubmitResult;
import dev.webfx.stack.platform.async.Batch;
import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface SubmitServiceProvider {

    Future<SubmitResult> executeSubmit(SubmitArgument argument);

    default Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        return batch.executeSerial(SubmitResult[]::new, this::executeSubmit);
    }

}
