// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.orm.reactive.entities {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.graphics;
    requires webfx.extras.type;
    requires webfx.framework.client.orm.reactive.dql;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.entity;
    requires webfx.framework.shared.orm.expression;
    requires webfx.kit.util;
    requires webfx.platform.shared.util;
    requires webfx.stack.db.query;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.client.orm.reactive.mapping.dql_to_entities;
    exports dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_grid;
    exports dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_objects;

}