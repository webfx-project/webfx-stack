// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql.submit.interceptor {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.submit;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.expression;

    // Exported packages
    exports dev.webfx.stack.orm.dql.submit.interceptor;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.orm.dql.submit.interceptor.DqlSubmitInterceptorModuleBooter;

}