// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.querypush.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.querypush;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.framework.server.services.querypush.spi.impl.simple;

    // Provided services
    provides dev.webfx.stack.framework.shared.services.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.framework.server.services.querypush.spi.impl.simple.SimpleInMemoryServerQueryPushServiceProvider;

}