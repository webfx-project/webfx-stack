// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.entity {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.expression;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.submit;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.shared.orm.entity;
    exports dev.webfx.framework.shared.orm.entity.impl;
    exports dev.webfx.framework.shared.orm.entity.lciimpl;
    exports dev.webfx.framework.shared.orm.entity.query_result_to_entities;
    exports dev.webfx.framework.shared.orm.entity.result;
    exports dev.webfx.framework.shared.orm.entity.result.impl;

    // Used services
    uses dev.webfx.framework.shared.orm.entity.EntityFactoryProvider;

}