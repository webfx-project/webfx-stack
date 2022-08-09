// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.router {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.stack.authz;

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