package dev.webfx.stack.db.submit;

import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class SubmitService {

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
