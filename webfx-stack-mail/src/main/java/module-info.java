// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.mail {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.service;

    // Exported packages
    exports dev.webfx.stack.mail;
    exports dev.webfx.stack.mail.spi;

    // Used services
    uses dev.webfx.stack.mail.spi.MailServiceProvider;

}