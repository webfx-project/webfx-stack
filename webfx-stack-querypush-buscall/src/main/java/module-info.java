// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush.buscall {

    // Direct dependencies modules
    requires webfx.platform.json;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.querypush;

    // Exported packages
    exports dev.webfx.stack.querypush.buscall;
    exports dev.webfx.stack.querypush.buscall.serial;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.querypush.buscall.ExecuteQueryPushMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.querypush.buscall.serial.QueryPushArgumentSerialCodec, dev.webfx.stack.querypush.buscall.serial.QueryPushResultSerialCodec, dev.webfx.stack.querypush.buscall.serial.QueryResultTranslationSerialCodec;

}