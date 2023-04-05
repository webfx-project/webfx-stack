// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.i18n {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.graphics;
    requires webfx.platform.async;
    requires webfx.platform.uischeduler;
    requires transitive webfx.platform.util;
    requires webfx.stack.ui.fxraiser;
    requires webfx.stack.ui.operation;

    // Exported packages
    exports dev.webfx.stack.i18n;
    exports dev.webfx.stack.i18n.operations;
    exports dev.webfx.stack.i18n.spi;
    exports dev.webfx.stack.i18n.spi.impl;

    // Used services
    uses dev.webfx.stack.i18n.operations.ChangeLanguageRequestEmitter;
    uses dev.webfx.stack.i18n.spi.I18nProvider;

}