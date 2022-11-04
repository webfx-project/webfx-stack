// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querysubmit.java.jdbc {

    // Direct dependencies modules
    requires static com.zaxxer.hikari;
    requires java.base;
    requires java.sql;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.query.remote;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submit.remote;

    // Exported packages
    exports dev.webfx.stack.db.query.spi.impl.jdbc;
    exports dev.webfx.stack.db.querysubmit.jdbc;
    exports dev.webfx.stack.db.submit.spi.impl.jdbc;

    // Provided services
    provides dev.webfx.stack.db.query.spi.QueryServiceProvider with dev.webfx.stack.db.query.spi.impl.jdbc.JdbcQueryServiceProvider;
    provides dev.webfx.stack.db.submit.spi.SubmitServiceProvider with dev.webfx.stack.db.submit.spi.impl.jdbc.JdbcSubmitServiceProvider;

}