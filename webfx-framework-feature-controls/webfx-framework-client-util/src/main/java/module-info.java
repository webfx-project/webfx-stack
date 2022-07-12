// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.util {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.imagestore;
    requires webfx.kit.launcher;
    requires webfx.kit.util;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.framework.client.ui.util.anim;
    exports dev.webfx.stack.framework.client.ui.util.background;
    exports dev.webfx.stack.framework.client.ui.util.border;
    exports dev.webfx.stack.framework.client.ui.util.image;
    exports dev.webfx.stack.framework.client.ui.util.layout;
    exports dev.webfx.stack.framework.client.ui.util.paint;
    exports dev.webfx.stack.framework.client.ui.util.scene;

}