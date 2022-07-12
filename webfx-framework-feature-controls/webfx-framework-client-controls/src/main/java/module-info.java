// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.controls {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.imagestore;
    requires webfx.extras.materialdesign;
    requires webfx.extras.type;
    requires webfx.framework.client.action;
    requires webfx.framework.client.activity;
    requires webfx.framework.client.i18n;
    requires webfx.framework.client.util;
    requires webfx.framework.client.validation;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.framework.client.ui.controls;
    exports dev.webfx.stack.framework.client.ui.controls.alert;
    exports dev.webfx.stack.framework.client.ui.controls.button;
    exports dev.webfx.stack.framework.client.ui.controls.dialog;

    // Resources packages
    opens images.s16.controls;

}