// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.router {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.authz;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.shared.router;
    exports dev.webfx.stack.framework.shared.router.auth;
    exports dev.webfx.stack.framework.shared.router.auth.authz;
    exports dev.webfx.stack.framework.shared.router.auth.impl;
    exports dev.webfx.stack.framework.shared.router.impl;
    exports dev.webfx.stack.framework.shared.router.session;
    exports dev.webfx.stack.framework.shared.router.session.impl;
    exports dev.webfx.stack.framework.shared.router.util;

}