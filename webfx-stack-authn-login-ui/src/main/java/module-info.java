// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.ui {

    // Direct dependencies modules
    requires java.base;
    requires javafx.graphics;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.authn.login.ui;
    exports dev.webfx.stack.authn.login.ui.spi;

    // Used services
    uses dev.webfx.stack.authn.login.ui.spi.LoginUiProvider;

}