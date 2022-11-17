// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires transitive webfx.platform.json;
    requires webfx.platform.util;
    requires transitive webfx.stack.session;

    // Exported packages
    exports dev.webfx.stack.routing.router;
    exports dev.webfx.stack.routing.router.session;
    exports dev.webfx.stack.routing.router.session.impl;
    exports dev.webfx.stack.routing.router.spi;
    exports dev.webfx.stack.routing.router.util;

    // Used services
    uses dev.webfx.stack.routing.router.spi.RouterFactoryProvider;

}