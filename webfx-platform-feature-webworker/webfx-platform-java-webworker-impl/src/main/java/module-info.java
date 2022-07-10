// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.java.webworker.impl {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.webworker;

    // Exported packages
    exports dev.webfx.stack.platform.webworker.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.webworker.spi.WorkerServiceProvider with dev.webfx.stack.platform.webworker.spi.impl.java.JavaWorkerServiceProvider;

}