// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.ui.validation {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.imagestore;
    requires webfx.extras.util.background;
    requires webfx.extras.util.border;
    requires webfx.extras.util.scene;
    requires webfx.kit.util;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.ui.validation;
    exports dev.webfx.stack.ui.validation.controlsfx.control.decoration;
    exports dev.webfx.stack.ui.validation.controlsfx.impl;
    exports dev.webfx.stack.ui.validation.controlsfx.impl.skin;
    exports dev.webfx.stack.ui.validation.controlsfx.tools;
    exports dev.webfx.stack.ui.validation.controlsfx.validation;
    exports dev.webfx.stack.ui.validation.controlsfx.validation.decoration;
    exports dev.webfx.stack.ui.validation.mvvmfx;
    exports dev.webfx.stack.ui.validation.mvvmfx.visualization;

    // Resources packages
    opens dev.webfx.stack.ui.validation.controlsfx.images;

}