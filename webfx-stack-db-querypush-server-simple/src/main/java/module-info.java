// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush.server.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.querypush;
    requires webfx.stack.db.querypush.server;

    // Exported packages
    exports dev.webfx.stack.db.querypush.server.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.db.querypush.server.spi.impl.simple.SimpleInMemoryServerQueryPushServiceProvider;

}