// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.push {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.push;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;

    // Exported packages
    exports dev.webfx.stack.framework.server.services.push;
    exports dev.webfx.stack.framework.server.services.push.spi;

    // Used services
    uses dev.webfx.stack.framework.server.services.push.spi.PushServerServiceProvider;

}