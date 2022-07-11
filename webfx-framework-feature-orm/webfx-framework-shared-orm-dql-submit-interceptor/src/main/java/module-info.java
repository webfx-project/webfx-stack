// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.dql.submit.interceptor {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.orm.datasourcemodelservice;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.expression;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.framework.shared.interceptors.dqlsubmit;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter with dev.webfx.stack.framework.shared.interceptors.dqlsubmit.DqlSubmitInterceptorModuleBooter;

}