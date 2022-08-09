// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush.server.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.querypush;
    requires webfx.stack.querypush.server;

    // Exported packages
    exports dev.webfx.stack.querypush.server.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.querypush.server.spi.impl.simple.SimpleInMemoryServerQueryPushServiceProvider;

}