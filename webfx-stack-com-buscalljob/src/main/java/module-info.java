// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.buscalljob {

    // Direct dependencies modules
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.log;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;

    // Exported packages
    exports dev.webfx.stack.com.buscall.job;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationJob with dev.webfx.stack.com.buscall.job.BusCallServerJob;

}