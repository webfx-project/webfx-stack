// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.session {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.service;

    // Exported packages
    exports dev.webfx.stack.session;
    exports dev.webfx.stack.session.spi;

    // Used services
    uses dev.webfx.stack.session.spi.SessionServiceProvider;

}