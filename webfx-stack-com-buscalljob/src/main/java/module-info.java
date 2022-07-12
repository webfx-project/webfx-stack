// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.buscalljob {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;

    // Exported packages
    exports dev.webfx.stack.com.buscall.job;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.com.buscall.job.BusCallServerJob;

}