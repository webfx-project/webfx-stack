// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.i18n.ast {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.resource;
    requires webfx.platform.util;
    requires webfx.stack.i18n;

    // Exported packages
    exports dev.webfx.stack.i18n.spi.impl.ast;

    // Provided services
    provides dev.webfx.stack.i18n.spi.I18nProvider with dev.webfx.stack.i18n.spi.impl.ast.AstI18nProvider;

}