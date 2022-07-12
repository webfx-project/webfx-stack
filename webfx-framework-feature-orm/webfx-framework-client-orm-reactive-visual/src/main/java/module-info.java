// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.orm.reactive.visual {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.extras.cell;
    requires webfx.extras.label;
    requires webfx.extras.type;
    requires webfx.extras.visual.base;
    requires webfx.framework.client.i18n;
    requires webfx.framework.client.orm.reactive.dql;
    requires webfx.framework.client.orm.reactive.entities;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.entity;
    requires webfx.framework.shared.orm.expression;
    requires webfx.platform.util;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual;
    exports dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual.conventions;

}