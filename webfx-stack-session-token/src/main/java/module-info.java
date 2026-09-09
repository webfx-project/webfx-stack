// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Mints and verifies the signed, expiring token that carries a caller's identity between
        the server and a client it does not trust. Server-only: the client treats the token as opaque and
        merely echoes it, so nothing here needs to survive GWT.
 */
module webfx.stack.session.token {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.stack.com.serial;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.session.token;

}