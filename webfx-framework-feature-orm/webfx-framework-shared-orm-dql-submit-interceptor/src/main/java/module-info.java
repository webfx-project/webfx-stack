// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.dql.submit.interceptor {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.shared.orm.datasourcemodelservice;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.expression;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.submit;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.framework.shared.interceptors.dqlsubmit;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter with dev.webfx.stack.framework.shared.interceptors.dqlsubmit.DqlSubmitInterceptorModuleBooter;

}