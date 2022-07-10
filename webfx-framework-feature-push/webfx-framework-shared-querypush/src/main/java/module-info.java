// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.querypush {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.buscall;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.serial;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.framework.shared.services.querypush;
    exports dev.webfx.stack.framework.shared.services.querypush.diff;
    exports dev.webfx.stack.framework.shared.services.querypush.diff.impl;
    exports dev.webfx.stack.framework.shared.services.querypush.spi;
    exports dev.webfx.stack.framework.shared.services.querypush.spi.impl;

    // Used services
    uses dev.webfx.stack.framework.shared.services.querypush.spi.QueryPushServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.framework.shared.services.querypush.QueryPushArgument.ProvidedSerialCodec, dev.webfx.stack.framework.shared.services.querypush.QueryPushResult.ProvidedSerialCodec, dev.webfx.stack.framework.shared.services.querypush.diff.impl.QueryResultTranslation.ProvidedSerialCodec;
    provides dev.webfx.stack.platform.shared.services.buscall.spi.BusCallEndpoint with dev.webfx.stack.framework.shared.services.querypush.ExecuteQueryPushBusCallEndpoint;

}