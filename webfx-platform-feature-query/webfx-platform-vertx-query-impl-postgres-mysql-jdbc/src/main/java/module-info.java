// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.query.impl.postgres.mysql.jdbc {

    // Direct dependencies modules
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.query;
    requires webfx.platform.vertx.queryupdate;

    // Exported packages
    exports dev.webfx.stack.platform.vertx.services.query.spi.impl;

    // Provided services
    provides dev.webfx.stack.platform.shared.services.query.spi.QueryServiceProvider with dev.webfx.stack.platform.vertx.services.query.spi.impl.VertxQueryServiceProvider;

}