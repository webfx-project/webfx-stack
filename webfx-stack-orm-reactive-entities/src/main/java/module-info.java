// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.reactive.entities {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.graphics;
    requires webfx.extras.type;
    requires webfx.kit.util;
    requires webfx.platform.ast.json.plugin;
    requires webfx.stack.db.query;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.expression;
    requires webfx.stack.orm.reactive.dql;

    // Exported packages
    exports dev.webfx.stack.orm.reactive.entities.dql_to_entities;
    exports dev.webfx.stack.orm.reactive.entities.entities_to_grid;
    exports dev.webfx.stack.orm.reactive.entities.entities_to_objects;

}