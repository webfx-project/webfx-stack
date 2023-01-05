// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submit.listener {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.db.submit.listener;

    // Used services
    uses dev.webfx.stack.db.submit.listener.SubmitListener;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.db.submit.listener.SubmitListenerModuleBooter;

}