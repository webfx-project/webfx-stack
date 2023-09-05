// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submit.buscall {

    // Direct dependencies modules
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.db.submit.buscall;
    exports dev.webfx.stack.db.submit.buscall.serial;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.db.submit.buscall.ExecuteSubmitMethodEndpoint, dev.webfx.stack.db.submit.buscall.ExecuteSubmitBatchMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.submit.buscall.serial.SubmitArgumentSerialCodec, dev.webfx.stack.db.submit.buscall.serial.SubmitResultSerialCodec, dev.webfx.stack.db.submit.buscall.serial.GeneratedKeyBatchIndexSerialCodec;

}