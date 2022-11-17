// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires transitive webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.db.querypush;
    exports dev.webfx.stack.db.querypush.diff;
    exports dev.webfx.stack.db.querypush.diff.impl;
    exports dev.webfx.stack.db.querypush.spi;
    exports dev.webfx.stack.db.querypush.spi.impl;

    // Used services
    uses dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;

}