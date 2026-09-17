// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * Mail transport abstraction: transmits a fully-addressed, fully-rendered
        email through a configurable provider (smtp, sandbox, later aws-ses). This is the
        layer BELOW webfx-stack-mail: MailService.sendMail() enqueues a message (in Modality,
        a Mail DB row), while MailTransport actually delivers one over the wire. Provider
        selection is config-driven (webfx.stack.mail.transport.provider) — several provider
        modules coexist on the classpath and the booter picks one by name, so the same
        artifact can run with real SMTP in production and the sandbox on staging. The SPI
        also declares delivery-feedback/suppression hooks (bounces, complaints) as inert
        defaults, to be filled by the SES provider without an API break.
 */
module webfx.stack.mail.transport {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.service;

    // Exported packages
    exports dev.webfx.stack.mail.transport;
    exports dev.webfx.stack.mail.transport.spi;

    // Used services
    uses dev.webfx.stack.mail.transport.spi.MailTransportProvider;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.mail.transport.MailTransportModuleBooter;

}