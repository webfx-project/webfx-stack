// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.orm.reactive.dql {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.framework.client.activity;
    requires webfx.framework.client.push;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.entity;
    requires webfx.framework.shared.orm.expression;
    requires webfx.framework.shared.querypush;
    requires webfx.kit.util;
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.client.orm.reactive.call;
    exports dev.webfx.framework.client.orm.reactive.call.query;
    exports dev.webfx.framework.client.orm.reactive.call.query.push;
    exports dev.webfx.framework.client.orm.reactive.dql.query;
    exports dev.webfx.framework.client.orm.reactive.dql.querypush;
    exports dev.webfx.framework.client.orm.reactive.dql.statement;
    exports dev.webfx.framework.client.orm.reactive.dql.statement.conventions;

}