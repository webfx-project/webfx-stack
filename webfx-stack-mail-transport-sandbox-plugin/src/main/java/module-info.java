// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Sandbox implementation of the mail transport SPI: swallows every message
        and logs a structured summary (recipients, subject, Message-ID; body only with
        logBody = true) instead of transmitting anything. This is the staging steady state —
        selected by webfx.stack.mail.transport.provider = sandbox, it guarantees by config
        that a prod→staging data refresh can never email real customers. Pure Java, no
        Vert.x dependency.
 */
module webfx.stack.mail.transport.sandbox.plugin {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.stack.mail.transport;

    // Exported packages
    exports dev.webfx.stack.mail.transport.spi.impl.sandbox;

    // Provided services
    provides dev.webfx.stack.mail.transport.spi.MailTransportProvider with dev.webfx.stack.mail.transport.spi.impl.sandbox.SandboxMailTransportProvider;

}