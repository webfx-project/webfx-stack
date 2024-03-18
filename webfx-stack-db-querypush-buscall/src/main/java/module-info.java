// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush.buscall {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.querypush;

    // Exported packages
    exports dev.webfx.stack.db.querypush.buscall;
    exports dev.webfx.stack.db.querypush.buscall.serial;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.db.querypush.buscall.ExecuteQueryPushMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.querypush.buscall.serial.QueryPushArgumentSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryPushResultSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryResultTranslationSerialCodec;

}