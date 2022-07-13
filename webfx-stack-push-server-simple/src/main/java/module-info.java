// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.push.server.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;
    requires webfx.stack.push;
    requires webfx.stack.push.server;

    // Exported packages
    exports dev.webfx.stack.push.server.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.push.server.spi.PushServerServiceProvider with dev.webfx.stack.push.server.spi.impl.simple.SimplePushServerServiceProvider;

}