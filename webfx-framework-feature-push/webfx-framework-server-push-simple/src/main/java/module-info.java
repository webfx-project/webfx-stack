// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.push.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.push;
    requires webfx.framework.shared.push;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;

    // Exported packages
    exports dev.webfx.stack.framework.server.services.push.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.framework.server.services.push.spi.PushServerServiceProvider with dev.webfx.stack.framework.server.services.push.spi.impl.simple.SimplePushServerServiceProvider;

}