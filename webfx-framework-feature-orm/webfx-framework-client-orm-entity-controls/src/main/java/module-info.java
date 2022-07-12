// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.orm.entity.controls {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.cell;
    requires webfx.extras.imagestore;
    requires webfx.extras.label;
    requires webfx.extras.materialdesign;
    requires webfx.extras.type;
    requires webfx.extras.visual;
    requires webfx.extras.visual.grid;
    requires webfx.framework.client.controls;
    requires webfx.framework.client.orm.reactive.entities;
    requires webfx.framework.client.orm.reactive.visual;
    requires webfx.framework.client.util;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.entity;
    requires webfx.framework.shared.orm.expression;
    requires webfx.kit.util;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.db.submit;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.client.ui.controls.entity.selector;
    exports dev.webfx.stack.framework.client.ui.controls.entity.sheet;

}