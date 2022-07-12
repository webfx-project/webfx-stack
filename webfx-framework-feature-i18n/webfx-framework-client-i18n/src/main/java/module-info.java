// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.i18n {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.framework.shared.operation;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.async;

    // Exported packages
    exports dev.webfx.stack.framework.client.operations.i18n;
    exports dev.webfx.stack.framework.client.services.i18n;
    exports dev.webfx.stack.framework.client.services.i18n.spi;
    exports dev.webfx.stack.framework.client.services.i18n.spi.impl;

    // Used services
    uses dev.webfx.stack.framework.client.operations.i18n.ChangeLanguageRequestEmitter;
    uses dev.webfx.stack.framework.client.services.i18n.spi.I18nProvider;

}