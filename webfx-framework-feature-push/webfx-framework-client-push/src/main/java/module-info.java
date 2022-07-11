// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.push {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.framework.shared.push;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;

    // Exported packages
    exports dev.webfx.stack.framework.client.services.push;
    exports dev.webfx.stack.framework.client.services.push.spi;

    // Used services
    uses dev.webfx.stack.framework.client.services.push.spi.PushClientServiceProvider;

}