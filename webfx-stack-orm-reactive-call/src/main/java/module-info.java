// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.reactive.call {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.stack.db.query;
    requires webfx.stack.db.querypush;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.orm.reactive.call;
    exports dev.webfx.stack.orm.reactive.call.query;
    exports dev.webfx.stack.orm.reactive.call.query.push;

}