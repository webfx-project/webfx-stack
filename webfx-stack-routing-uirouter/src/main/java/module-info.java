// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.routing.uirouter {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.graphics;
    requires webfx.extras.i18n;
    requires webfx.extras.operation;
    requires webfx.kit.launcher;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.service;
    requires webfx.platform.uischeduler;
    requires transitive webfx.platform.util;
    requires transitive webfx.platform.windowhistory;
    requires transitive webfx.stack.routing.activity;
    requires webfx.stack.routing.router;
    requires transitive webfx.stack.routing.router.client;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.routing.uirouter;
    exports dev.webfx.stack.routing.uirouter.activity.presentation;
    exports dev.webfx.stack.routing.uirouter.activity.presentation.impl;
    exports dev.webfx.stack.routing.uirouter.activity.presentation.logic;
    exports dev.webfx.stack.routing.uirouter.activity.presentation.logic.impl;
    exports dev.webfx.stack.routing.uirouter.activity.presentation.view;
    exports dev.webfx.stack.routing.uirouter.activity.presentation.view.impl;
    exports dev.webfx.stack.routing.uirouter.activity.uiroute;
    exports dev.webfx.stack.routing.uirouter.activity.uiroute.impl;
    exports dev.webfx.stack.routing.uirouter.activity.view;
    exports dev.webfx.stack.routing.uirouter.activity.view.impl;
    exports dev.webfx.stack.routing.uirouter.impl;
    exports dev.webfx.stack.routing.uirouter.operations;

    // Used services
    uses dev.webfx.stack.routing.uirouter.UiRoute;
    uses dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter;

}