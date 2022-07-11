// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.scheduler.vertx {

    // Direct dependencies modules
    requires io.vertx.core;
    requires webfx.platform.shared.scheduler;
    requires webfx.stack.vertx.common;

    // Exported packages
    exports dev.webfx.platform.scheduler.spi.impl.vertx;

    // Provided services
    provides dev.webfx.platform.shared.services.scheduler.spi.SchedulerProvider with dev.webfx.platform.scheduler.spi.impl.vertx.VertxSchedulerProvider;

}