// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.datasource {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.db.datasource;
    exports dev.webfx.stack.db.datasource.jdbc;
    exports dev.webfx.stack.db.datasource.spi;
    exports dev.webfx.stack.db.datasource.spi.simple;

    // Used services
    uses dev.webfx.stack.db.datasource.spi.LocalDataSourceProvider;

}