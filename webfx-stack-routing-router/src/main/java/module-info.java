// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.authz;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.routing.router;
    exports dev.webfx.stack.routing.router.auth;
    exports dev.webfx.stack.routing.router.auth.authz;
    exports dev.webfx.stack.routing.router.auth.impl;
    exports dev.webfx.stack.routing.router.impl;
    exports dev.webfx.stack.routing.router.session;
    exports dev.webfx.stack.routing.router.session.impl;
    exports dev.webfx.stack.routing.router.util;

}