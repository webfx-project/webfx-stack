// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.webworker.java {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.log;
    requires webfx.stack.platform.json;
    requires webfx.stack.platform.webworker;

    // Exported packages
    exports dev.webfx.stack.platform.webworker.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.webworker.spi.WorkerServiceProvider with dev.webfx.stack.platform.webworker.spi.impl.java.JavaWorkerServiceProvider;

}