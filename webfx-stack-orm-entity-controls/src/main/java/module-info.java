// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.entity.controls {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.cell;
    requires webfx.extras.imagestore;
    requires webfx.extras.label;
    requires webfx.extras.materialdesign;
    requires webfx.extras.panes;
    requires webfx.extras.type;
    requires webfx.extras.util.border;
    requires webfx.extras.util.layout;
    requires webfx.extras.util.scene;
    requires webfx.extras.visual;
    requires webfx.extras.visual.grid;
    requires webfx.kit.util;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.db.submit;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.expression;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.orm.reactive.visual;
    requires transitive webfx.stack.ui.controls;
    requires webfx.stack.ui.dialog;

    // Exported packages
    exports dev.webfx.stack.orm.entity.controls.entity.selector;
    exports dev.webfx.stack.orm.entity.controls.entity.sheet;

}