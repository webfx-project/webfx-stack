// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * SMTP implementation of the mail transport SPI, built on the Vert.x mail
        client. The SMTP route (host/port/ssl/credentials) is resolved PER MESSAGE from the
        TransportTags carried by the TransportMessage — in Modality these come from the
        per-account SmtpAccount DB rows — with optional config fallbacks
        (webfx.stack.mail.transport.smtp.defaultHost etc.) for deployments with a single
        fixed relay. Mail clients are cached per route. Selected at runtime by
        webfx.stack.mail.transport.provider = smtp.
 */
module webfx.stack.mail.transport.smtp.vertx.plugin {

    // Direct dependencies modules
    requires io.vertx.core;
    requires io.vertx.mail.client;
    requires webfx.platform.async;
    requires webfx.platform.conf;
    requires webfx.platform.util.vertx;
    requires webfx.stack.mail.transport;

    // Exported packages
    exports dev.webfx.stack.mail.transport.spi.impl.smtp.vertx;

    // Provided services
    provides dev.webfx.stack.mail.transport.spi.MailTransportProvider with dev.webfx.stack.mail.transport.spi.impl.smtp.vertx.SmtpVertxMailTransportProvider;

}