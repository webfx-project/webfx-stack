// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.querypush.simple {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.querypush;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.server.services.querypush.spi.impl.simple;

    // Provided services
    provides dev.webfx.framework.shared.services.querypush.spi.QueryPushServiceProvider with dev.webfx.framework.server.services.querypush.spi.impl.simple.SimpleInMemoryServerQueryPushServiceProvider;

}