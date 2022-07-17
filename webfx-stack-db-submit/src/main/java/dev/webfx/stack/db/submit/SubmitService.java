package dev.webfx.stack.db.submit;

import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.async.Batch;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class SubmitService {

    public static final String SUBMIT_SERVICE_ADDRESS = "service/submit";
    public static final String SUBMIT_BATCH_SERVICE_ADDRESS = "service/submit/batch";

    public static SubmitServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(SubmitServiceProvider.class, () -> ServiceLoader.load(SubmitServiceProvider.class));
    }

    public static Future<SubmitResult> executeSubmit(SubmitArgument argument) {
        return getProvider().executeSubmit(argument);
    }

    public static  Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        return getProvider().executeSubmitBatch(batch);
    }

}