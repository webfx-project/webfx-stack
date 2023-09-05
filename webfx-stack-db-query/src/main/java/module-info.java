// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires transitive webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;

    // Exported packages
    exports dev.webfx.stack.db.query;
    exports dev.webfx.stack.db.query.spi;
    exports dev.webfx.stack.db.query.spi.impl;

    // Used services
    uses dev.webfx.stack.db.query.spi.QueryServiceProvider;

}