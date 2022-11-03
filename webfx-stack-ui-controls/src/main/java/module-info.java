// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.ui.controls {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.imagestore;
    requires webfx.extras.materialdesign;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.i18n;
    requires transitive webfx.stack.ui.action;
    requires webfx.stack.ui.util;
    requires webfx.stack.ui.validation;

    // Exported packages
    exports dev.webfx.stack.ui.controls;
    exports dev.webfx.stack.ui.controls.alert;
    exports dev.webfx.stack.ui.controls.button;
    exports dev.webfx.stack.ui.controls.dialog;

    // Resources packages
    opens images.s16.controls;

}