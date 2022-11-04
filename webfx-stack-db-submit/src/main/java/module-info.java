// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submit {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.util;
    requires transitive webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;

    // Exported packages
    exports dev.webfx.stack.db.submit;
    exports dev.webfx.stack.db.submit.spi;
    exports dev.webfx.stack.db.submit.spi.impl;

    // Used services
    uses dev.webfx.stack.db.submit.spi.SubmitServiceProvider;

}