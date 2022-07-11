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
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.client.orm.reactive.call;
    exports dev.webfx.stack.framework.client.orm.reactive.call.query;
    exports dev.webfx.stack.framework.client.orm.reactive.call.query.push;
    exports dev.webfx.stack.framework.client.orm.reactive.dql.query;
    exports dev.webfx.stack.framework.client.orm.reactive.dql.querypush;
    exports dev.webfx.stack.framework.client.orm.reactive.dql.statement;
    exports dev.webfx.stack.framework.client.orm.reactive.dql.statement.conventions;

}