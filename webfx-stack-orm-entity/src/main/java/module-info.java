// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.entity {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires transitive webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.expression;

    // Exported packages
    exports dev.webfx.stack.orm.entity;
    exports dev.webfx.stack.orm.entity.impl;
    exports dev.webfx.stack.orm.entity.lciimpl;
    exports dev.webfx.stack.orm.entity.query_result_to_entities;
    exports dev.webfx.stack.orm.entity.result;
    exports dev.webfx.stack.orm.entity.result.impl;

    // Used services
    uses dev.webfx.stack.orm.entity.EntityFactoryProvider;

}