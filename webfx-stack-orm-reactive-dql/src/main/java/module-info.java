// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.reactive.dql {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.kit.util;
    requires webfx.platform.console;
    requires webfx.platform.json;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.expression;
    requires webfx.stack.orm.reactive.call;
    requires webfx.stack.routing.activity;

    // Exported packages
    exports dev.webfx.stack.orm.reactive.dql.query;
    exports dev.webfx.stack.orm.reactive.dql.querypush;
    exports dev.webfx.stack.orm.reactive.dql.statement;
    exports dev.webfx.stack.orm.reactive.dql.statement.conventions;

}