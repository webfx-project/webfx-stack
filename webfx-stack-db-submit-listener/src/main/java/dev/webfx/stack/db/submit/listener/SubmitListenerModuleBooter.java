package dev.webfx.stack.db.submit.listener;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.collection.Collections;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class SubmitListenerModuleBooter implements ApplicationModuleBooter {

    private List<SubmitListener> providedListener;

    @Override
    public String getModuleName() {
        return "webfx-stack-db-submit-listener";
    }

    @Override
    public int getBootLevel() {
        return JOBS_START_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        providedListener = Collections.listOf(ServiceLoader.load(SubmitListener.class));
        providedListener.forEach(SubmitListenerService::addSubmitListener);
        Console.log(providedListener.size() + " submit listeners found and registered:");
    }

    @Override
    public void exitModule() {
        providedListener.forEach(SubmitListenerService::removeSubmitListener);
    }
}
