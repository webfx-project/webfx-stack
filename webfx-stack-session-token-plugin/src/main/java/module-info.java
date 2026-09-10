// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Installs the session-token signing keys from configuration. Separate from
        webfx-stack-session-token so the primitive itself keeps no dependencies and stays testable without
        a configured stack.
 */
module webfx.stack.session.token.plugin {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.meta;
    requires webfx.platform.substitution;
    requires webfx.stack.session.token;

    // Exported packages
    exports dev.webfx.stack.session.token.plugin;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.session.token.plugin.SessionTokenKeysInitializer;

}