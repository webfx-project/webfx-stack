// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.i18n.time.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.extras.time.format;
    requires webfx.stack.i18n;

    // Exported packages
    exports dev.webfx.stack.i18n.time;

    // Provided services
    provides dev.webfx.extras.time.format.spi.TimeFormatProvider with dev.webfx.stack.i18n.time.I18nTimeFormatProvider;

}