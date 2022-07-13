// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.call.job {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.com.bus.call.job;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.com.bus.call.job.BusCallServerJob;

}