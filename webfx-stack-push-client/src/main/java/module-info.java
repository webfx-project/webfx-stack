// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.push.client {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.push;

    // Exported packages
    exports dev.webfx.stack.orm.push.client;
    exports dev.webfx.stack.orm.push.client.spi;

    // Used services
    uses dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider;

}