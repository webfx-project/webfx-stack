// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql.query.interceptor {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;

    // Exported packages
    exports dev.webfx.stack.orm.dql.query.interceptor;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.orm.dql.query.interceptor.DqlQueryInterceptorModuleBooter;

}