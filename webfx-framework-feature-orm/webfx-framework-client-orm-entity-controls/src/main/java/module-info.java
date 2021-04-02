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
    requires webfx.extras.visual.base;
    requires webfx.extras.visual.controls.grid;
    requires webfx.framework.client.controls;
    requires webfx.framework.client.orm.reactive.entities;
    requires webfx.framework.client.orm.reactive.visual;
    requires webfx.framework.client.util;
    requires webfx.framework.shared.orm.domainmodel;
    requires webfx.framework.shared.orm.dql;
    requires webfx.framework.shared.orm.entity;
    requires webfx.framework.shared.orm.expression;
    requires webfx.kit.util;
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.submit;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.client.ui.controls.entity.selector;
    exports dev.webfx.framework.client.ui.controls.entity.sheet;

}