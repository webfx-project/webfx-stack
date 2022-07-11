// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.entity {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.expression;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.framework.shared.orm.entity;
    exports dev.webfx.stack.framework.shared.orm.entity.impl;
    exports dev.webfx.stack.framework.shared.orm.entity.lciimpl;
    exports dev.webfx.stack.framework.shared.orm.entity.query_result_to_entities;
    exports dev.webfx.stack.framework.shared.orm.entity.result;
    exports dev.webfx.stack.framework.shared.orm.entity.result.impl;

    // Used services
    uses dev.webfx.stack.framework.shared.orm.entity.EntityFactoryProvider;

}