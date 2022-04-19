// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.push.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.push;
    requires webfx.framework.shared.push;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.buscall;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.scheduler;

    // Exported packages
    exports dev.webfx.framework.server.services.push.spi.impl.simple;

    // Provided services
    provides dev.webfx.framework.server.services.push.spi.PushServerServiceProvider with dev.webfx.framework.server.services.push.spi.impl.simple.SimplePushServerServiceProvider;

}