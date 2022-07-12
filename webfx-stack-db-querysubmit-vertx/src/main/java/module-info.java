// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querysubmit.vertx {

    // Direct dependencies modules
    requires io.vertx.client.jdbc;
    requires io.vertx.client.sql;
    requires io.vertx.client.sql.pg;
    requires io.vertx.core;
    requires java.base;
    requires java.sql;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submitlistener;
    requires webfx.stack.vertx.common;

    // Exported packages
    exports dev.webfx.stack.db.query.spi.impl.vertx;
    exports dev.webfx.stack.db.querysubmit;
    exports dev.webfx.stack.db.submit.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.db.query.spi.QueryServiceProvider with dev.webfx.stack.db.query.spi.impl.vertx.VertxQueryServiceProvider;
    provides dev.webfx.stack.db.submit.spi.SubmitServiceProvider with dev.webfx.stack.db.submit.spi.impl.vertx.VertxSubmitServiceProvider;

}