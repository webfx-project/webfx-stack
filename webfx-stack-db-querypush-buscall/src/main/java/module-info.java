// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush.buscall {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.querypush;

    // Exported packages
    exports dev.webfx.stack.db.querypush.buscall;
    exports dev.webfx.stack.db.querypush.buscall.serial;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.db.querypush.buscall.ArmSqlAnalyzeMethodEndpoint, dev.webfx.stack.db.querypush.buscall.CancelSqlQueryMethodEndpoint, dev.webfx.stack.db.querypush.buscall.ExecuteQueryPushMethodEndpoint, dev.webfx.stack.db.querypush.buscall.GetDatabaseHealthInfoMethodEndpoint, dev.webfx.stack.db.querypush.buscall.GetQueryPushMonitorInfoMethodEndpoint, dev.webfx.stack.db.querypush.buscall.GetSqlAnalyzeResultMethodEndpoint, dev.webfx.stack.db.querypush.buscall.ResetSqlAnalyzeMethodEndpoint, dev.webfx.stack.db.querypush.buscall.ResetSqlMonitorMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.querypush.buscall.serial.ActiveDbQueryInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.BootJobFailureMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.CompressionMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.DatabaseHealthMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.DbErrorMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.NameCountInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryPushArgumentSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryPushMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryPushResultSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryResultTranslationSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.QueryStreamMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.InFlightQueryMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.SqlAnalyzeResultInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.SqlExecutionMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.SqlKindMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.StatementMonitorInfoSerialCodec, dev.webfx.stack.db.querypush.buscall.serial.SystemResourceMonitorInfoSerialCodec;

}