// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * A headless command-line DQL client: signs in as a real caller, holds that caller's session, and runs DQL over the bus through exactly the path a browser app uses.
 */
module webfx.stack.db.query.cli {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.conf;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.stack.authn;
    requires webfx.stack.authn.buscall;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.bus.json.client.websocket;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.query.remote;
    requires webfx.stack.push;
    requires webfx.stack.session.state;
    requires webfx.stack.session.state.client;

    // Exported packages
    exports dev.webfx.stack.db.query.cli;

    // Used services
    uses dev.webfx.stack.db.query.spi.QueryServiceProvider;

    // Provided services
    provides dev.webfx.platform.uischeduler.spi.UiSchedulerProvider with dev.webfx.stack.db.query.cli.HeadlessUiSchedulerProvider;

}