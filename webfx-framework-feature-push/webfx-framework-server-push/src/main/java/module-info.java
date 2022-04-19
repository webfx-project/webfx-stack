// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.push {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.push;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.server.services.push;
    exports dev.webfx.framework.server.services.push.spi;

    // Used services
    uses dev.webfx.framework.server.services.push.spi.PushServerServiceProvider;

}