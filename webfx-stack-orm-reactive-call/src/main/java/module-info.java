// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.reactive.call {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.kit.util;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.stack.db.query;
    requires webfx.stack.db.querypush;
    requires webfx.stack.session.state.client.fx;
    requires webfx.stack.shareddata.cache;
    requires webfx.stack.shareddata.cache.serial;

    // Exported packages
    exports dev.webfx.stack.orm.reactive.call;
    exports dev.webfx.stack.orm.reactive.call.query;
    exports dev.webfx.stack.orm.reactive.call.query.push;

}