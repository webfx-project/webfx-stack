// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Authorization rule model and evaluation, shared by the client (UI affordances) and the
        server (enforcement). No JavaFX, no I/O, no ambient state — every input is a parameter, so it can
        be unit-tested without a running stack.
 */
module webfx.stack.authz.core {

    // Direct dependencies modules
    requires webfx.extras.operation;
    requires webfx.platform.async;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.authz.core;
    exports dev.webfx.stack.authz.core.operation;
    exports dev.webfx.stack.authz.core.parser;

}