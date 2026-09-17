// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * 
        Applies pending database migration scripts at server boot, all in a single transaction, gating the
        application readiness (/health) until done. The migration scripts are bundled in the application jar
        and contributed via the MigrationScriptsProvider SPI; the statements are executed through the generic
        SubmitService API, so this module stays platform-neutral. On failure, everything is rolled back and
        the application never becomes ready, which makes a blue/green deployment roll back to the previous
        version while the database is left untouched.
    
 */
module webfx.stack.db.migration {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.service;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.db.migration;
    exports dev.webfx.stack.db.migration.spi;

    // Used services
    uses dev.webfx.stack.db.migration.spi.MigrationScriptsProvider;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.db.migration.DbMigrationJob;

}