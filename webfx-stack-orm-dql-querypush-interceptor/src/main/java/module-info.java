// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql.querypush.interceptor {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.service;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.querypush;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.expression;

    // Exported packages
    exports dev.webfx.stack.orm.dql.querypush.interceptor;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.orm.dql.querypush.interceptor.DqlQueryPushInterceptorInitializer;

}