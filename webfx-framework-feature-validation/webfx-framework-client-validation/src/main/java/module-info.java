// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.validation {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    // Exported packages
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.control.decoration;
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.impl;
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.impl.skin;
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.tools;
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.validation;
    exports dev.webfx.stack.framework.client.ui.validation.controlsfx.validation.decoration;
    exports dev.webfx.stack.framework.client.ui.validation.mvvmfx;
    exports dev.webfx.stack.framework.client.ui.validation.mvvmfx.visualization;

    // Resources packages
    opens dev.webfx.stack.framework.client.ui.validation.controlsfx.images;

}