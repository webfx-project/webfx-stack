package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.vertx.common.VertxInstance;

/**
 * @author Bruno Salmon
 */
public final class VertxHttpModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-platform-boot-vertx";
    }

    @Override
    public int getBootLevel() {
        return ApplicationModuleBooter.COMMUNICATION_OPEN_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        VertxHttpRouterFinaliser.finaliseVertxHttpRouter();
        VertxInstance.getVertx().deployVerticle(new VertxHttpVerticle());
    }
}
