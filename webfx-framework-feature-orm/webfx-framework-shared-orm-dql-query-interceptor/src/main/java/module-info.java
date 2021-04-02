// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.dql.query.interceptor {

    // Direct dependencies modules
    requires webfx.framework.shared.orm.datasourcemodelservice;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.platform.shared.appcontainer;
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.shared.interceptors.dqlquery;

    // Provided services
    provides dev.webfx.platform.shared.services.appcontainer.spi.ApplicationModuleInitializer with dev.webfx.framework.shared.interceptors.dqlquery.DqlQueryInterceptorModuleInitializer;

}