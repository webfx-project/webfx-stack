// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires transitive webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.service;
    requires transitive webfx.stack.session;

    // Exported packages
    exports dev.webfx.stack.routing.router;
    exports dev.webfx.stack.routing.router.spi;
    exports dev.webfx.stack.routing.router.util;

    // Used services
    uses dev.webfx.stack.routing.router.spi.RouterFactoryProvider;

}