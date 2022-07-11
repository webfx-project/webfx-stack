// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.querypush {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.buscall;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.shared.services.querypush;
    exports dev.webfx.stack.framework.shared.services.querypush.diff;
    exports dev.webfx.stack.framework.shared.services.querypush.diff.impl;
    exports dev.webfx.stack.framework.shared.services.querypush.spi;
    exports dev.webfx.stack.framework.shared.services.querypush.spi.impl;

    // Used services
    uses dev.webfx.stack.framework.shared.services.querypush.spi.QueryPushServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.buscall.spi.BusCallEndpoint with dev.webfx.stack.framework.shared.services.querypush.ExecuteQueryPushBusCallEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.framework.shared.services.querypush.QueryPushArgument.ProvidedSerialCodec, dev.webfx.stack.framework.shared.services.querypush.QueryPushResult.ProvidedSerialCodec, dev.webfx.stack.framework.shared.services.querypush.diff.impl.QueryResultTranslation.ProvidedSerialCodec;

}