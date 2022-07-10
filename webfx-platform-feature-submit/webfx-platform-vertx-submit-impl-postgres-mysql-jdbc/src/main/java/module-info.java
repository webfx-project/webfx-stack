// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.vertx.submit.impl.postgres.mysql.jdbc {

    // Direct dependencies modules
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.submit;
    requires webfx.platform.vertx.queryupdate;

    // Exported packages
    exports dev.webfx.stack.platform.vertx.services.submit.spi.impl;

    // Provided services
    provides dev.webfx.stack.platform.shared.services.submit.spi.SubmitServiceProvider with dev.webfx.stack.platform.vertx.services.submit.spi.impl.VertxSubmitServiceProvider;

}