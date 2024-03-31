// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.push.server {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.stack.com.bus;
    requires webfx.stack.push;

    // Exported packages
    exports dev.webfx.stack.push.server;
    exports dev.webfx.stack.push.server.spi;

    // Used services
    uses dev.webfx.stack.push.server.spi.PushServerServiceProvider;

}