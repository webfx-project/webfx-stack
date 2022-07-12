// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.uirouter {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.graphics;
    requires webfx.framework.client.activity;
    requires webfx.framework.shared.authz;
    requires webfx.framework.shared.operation;
    requires webfx.framework.shared.router;
    requires webfx.kit.launcher;
    requires webfx.kit.util;
    requires webfx.platform.console;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;
    requires webfx.stack.platform.windowhistory;

    // Exported packages
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation.impl;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation.logic.impl;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.impl;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.uiroute;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.impl;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.view;
    exports dev.webfx.stack.framework.client.activity.impl.elementals.view.impl;
    exports dev.webfx.stack.framework.client.operations.route;
    exports dev.webfx.stack.framework.client.ui.uirouter;
    exports dev.webfx.stack.framework.client.ui.uirouter.impl;
    exports dev.webfx.stack.framework.client.ui.uirouter.uisession;
    exports dev.webfx.stack.framework.client.ui.uirouter.uisession.impl;

    // Used services
    uses dev.webfx.stack.framework.client.operations.route.RouteRequestEmitter;
    uses dev.webfx.stack.framework.client.ui.uirouter.UiRoute;

}