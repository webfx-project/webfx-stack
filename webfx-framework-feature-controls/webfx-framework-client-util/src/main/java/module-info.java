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
    requires webfx.platform.client.uischeduler;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.client.ui.util.anim;
    exports dev.webfx.framework.client.ui.util.background;
    exports dev.webfx.framework.client.ui.util.border;
    exports dev.webfx.framework.client.ui.util.image;
    exports dev.webfx.framework.client.ui.util.layout;
    exports dev.webfx.framework.client.ui.util.paint;
    exports dev.webfx.framework.client.ui.util.scene;

}