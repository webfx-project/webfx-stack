// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.buscalljob {

    // Direct dependencies modules
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.buscall;
    requires webfx.platform.shared.log;

    // Exported packages
    exports dev.webfx.stack.platform.server.jobs.buscall;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationJob with dev.webfx.stack.platform.server.jobs.buscall.BusCallServerJob;

}