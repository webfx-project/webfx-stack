// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.reactive.visual {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.extras.cell;
    requires webfx.extras.label;
    requires webfx.extras.type;
    requires webfx.extras.visual;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.util;
    requires webfx.stack.i18n;
    requires webfx.stack.orm.domainmodel;
    requires transitive webfx.stack.orm.entity;
    requires webfx.stack.orm.expression;
    requires webfx.stack.orm.reactive.dql;
    requires webfx.stack.orm.reactive.entities;

    // Exported packages
    exports dev.webfx.stack.orm.reactive.mapping.entities_to_visual;
    exports dev.webfx.stack.orm.reactive.mapping.entities_to_visual.conventions;

}