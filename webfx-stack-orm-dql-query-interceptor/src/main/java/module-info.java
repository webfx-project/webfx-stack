// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql.query.interceptor {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.service;
    requires webfx.platform.util;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.expression;

    // Exported packages
    exports dev.webfx.stack.orm.dql.query.interceptor;
    exports dev.webfx.stack.orm.dql.query.interceptor.serial;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.orm.dql.query.interceptor.DqlQueryInterceptorInitializer;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.orm.dql.query.interceptor.serial.QueryColumnToEntityFieldMappingSerialCodec, dev.webfx.stack.orm.dql.query.interceptor.serial.QueryRowToEntityMappingSerialCodec;

}