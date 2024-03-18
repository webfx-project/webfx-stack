// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.ui.controls {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.materialdesign;
    requires webfx.extras.util.background;
    requires webfx.extras.util.border;
    requires webfx.extras.util.layout;
    requires webfx.extras.util.paint;
    requires webfx.kit.util;
    requires webfx.platform.util;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;
    requires transitive webfx.stack.ui.action;
    requires transitive webfx.stack.ui.dialog;
    requires webfx.stack.ui.json;
    requires webfx.stack.ui.validation;

    // Exported packages
    exports dev.webfx.stack.ui.controls;
    exports dev.webfx.stack.ui.controls.alert;
    exports dev.webfx.stack.ui.controls.button;
    exports dev.webfx.stack.ui.controls.dialog;

    // Resources packages
    opens images.s16.controls;

}