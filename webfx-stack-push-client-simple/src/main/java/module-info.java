// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.push.client.simple {

    // Direct dependencies modules
    requires webfx.platform.console;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.push;
    requires webfx.stack.push.client;
    requires webfx.stack.session.state.client;

    // Exported packages
    exports dev.webfx.stack.orm.push.client.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider with dev.webfx.stack.orm.push.client.spi.impl.simple.SimplePushClientServiceProvider;

}