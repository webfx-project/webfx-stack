// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.com.buscall;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.querypush;
    exports dev.webfx.stack.querypush.diff;
    exports dev.webfx.stack.querypush.diff.impl;
    exports dev.webfx.stack.querypush.spi;
    exports dev.webfx.stack.querypush.spi.impl;

    // Used services
    uses dev.webfx.stack.querypush.spi.QueryPushServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.buscall.spi.BusCallEndpoint with dev.webfx.stack.querypush.ExecuteQueryPushBusCallEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.querypush.QueryPushArgument.ProvidedSerialCodec, dev.webfx.stack.querypush.QueryPushResult.ProvidedSerialCodec, dev.webfx.stack.querypush.diff.impl.QueryResultTranslation.ProvidedSerialCodec;

}