// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submitlistener {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.db.submitlistener;

    // Used services
    uses dev.webfx.stack.db.submitlistener.SubmitListener;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.db.submitlistener.SubmitListenerModuleBooter;

}