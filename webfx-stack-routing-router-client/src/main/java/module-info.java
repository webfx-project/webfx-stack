// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router.client {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.authz.client;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.state.client;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.routing.router.auth;
    exports dev.webfx.stack.routing.router.auth.authz;
    exports dev.webfx.stack.routing.router.auth.impl;
    exports dev.webfx.stack.routing.router.spi.impl.client;

    // Provided services
    provides dev.webfx.stack.routing.router.spi.RouterFactoryProvider with dev.webfx.stack.routing.router.spi.impl.client.ClientRouterFactoryProvider;

}