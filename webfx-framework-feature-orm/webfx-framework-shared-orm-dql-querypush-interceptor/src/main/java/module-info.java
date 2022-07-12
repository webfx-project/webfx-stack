// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.dql.querypush.interceptor {

    // Direct dependencies modules
    requires webfx.framework.shared.orm.datasourcemodelservice;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.expression;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.framework.shared.interceptors.dqlquerypush;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.framework.shared.interceptors.dqlquerypush.DqlQueryPushInterceptorModuleBooter;

}