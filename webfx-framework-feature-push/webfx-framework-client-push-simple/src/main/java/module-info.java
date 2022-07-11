// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.push.simple {

    // Direct dependencies modules
    requires webfx.framework.client.push;
    requires webfx.framework.shared.push;
    requires webfx.platform.shared.log;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.buscall;

    // Exported packages
    exports dev.webfx.stack.framework.client.services.push.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.framework.client.services.push.spi.PushClientServiceProvider with dev.webfx.stack.framework.client.services.push.spi.impl.simple.SimplePushClientServiceProvider;

}