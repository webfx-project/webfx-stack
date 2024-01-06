// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.entity {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires transitive webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.cache;
    requires transitive webfx.stack.db.datascope;
    requires transitive webfx.stack.db.query;
    requires transitive webfx.stack.db.submit;
    requires webfx.stack.orm.datasourcemodel.service;
    requires transitive webfx.stack.orm.domainmodel;
    requires transitive webfx.stack.orm.dql;
    requires transitive webfx.stack.orm.expression;

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