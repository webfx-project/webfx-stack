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
    exports dev.webfx.framework.shared.services.querypush;
    exports dev.webfx.framework.shared.services.querypush.diff;
    exports dev.webfx.framework.shared.services.querypush.diff.impl;
    exports dev.webfx.framework.shared.services.querypush.spi;
    exports dev.webfx.framework.shared.services.querypush.spi.impl;

    // Used services
    uses dev.webfx.framework.shared.services.querypush.spi.QueryPushServiceProvider;

    // Provided services
    provides dev.webfx.platform.shared.services.buscall.spi.BusCallEndpoint with dev.webfx.framework.shared.services.querypush.ExecuteQueryPushBusCallEndpoint;
    provides dev.webfx.platform.shared.services.serial.spi.SerialCodec with dev.webfx.framework.shared.services.querypush.QueryPushArgument.ProvidedSerialCodec, dev.webfx.framework.shared.services.querypush.QueryPushResult.ProvidedSerialCodec, dev.webfx.framework.shared.services.querypush.diff.impl.QueryResultTranslation.ProvidedSerialCodec;

}