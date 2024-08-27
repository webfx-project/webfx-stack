// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querysubmit.vertx {

    // Direct dependencies modules
    requires io.vertx.client.jdbc;
    requires io.vertx.client.sql;
    requires io.vertx.client.sql.pg;
    requires io.vertx.core;
    requires java.sql;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.platform.vertx.common;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submit.listener;
    requires webfx.platform.shutdown;

    // Exported packages
    exports dev.webfx.stack.db.query.spi.impl.vertx;
    exports dev.webfx.stack.db.querysubmit;
    exports dev.webfx.stack.db.submit.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.db.query.spi.QueryServiceProvider with dev.webfx.stack.db.query.spi.impl.vertx.VertxLocalQueryServiceProvider;
    provides dev.webfx.stack.db.submit.spi.SubmitServiceProvider with dev.webfx.stack.db.submit.spi.impl.vertx.VertxLocalSubmitServiceProvider;

}