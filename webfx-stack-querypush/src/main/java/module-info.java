// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires transitive webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.querypush;
    exports dev.webfx.stack.querypush.diff;
    exports dev.webfx.stack.querypush.diff.impl;
    exports dev.webfx.stack.querypush.spi;
    exports dev.webfx.stack.querypush.spi.impl;

    // Used services
    uses dev.webfx.stack.querypush.spi.QueryPushServiceProvider;

}