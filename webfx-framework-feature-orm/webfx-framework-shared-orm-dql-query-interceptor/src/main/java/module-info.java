// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.dql.query.interceptor {

    // Direct dependencies modules
    requires webfx.framework.shared.orm.datasourcemodelservice;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.framework.shared.interceptors.dqlquery;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.framework.shared.interceptors.dqlquery.DqlQueryInterceptorModuleBooter;

}