// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.queryupdate {

    // Direct dependencies modules
    requires io.vertx.client.jdbc;
    requires io.vertx.client.sql;
    requires io.vertx.client.sql.pg;
    requires io.vertx.core;
    requires java.base;
    requires java.sql;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.submit;
    requires webfx.platform.shared.submitlistener;
    requires webfx.platform.shared.util;
    requires webfx.platform.vertx.instance;

    // Exported packages
    exports dev.webfx.stack.platform.vertx.services_shared_code.queryupdate;

}