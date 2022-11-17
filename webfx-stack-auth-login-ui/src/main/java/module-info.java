// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login.ui {

    // Direct dependencies modules
    requires java.base;
    requires javafx.graphics;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.auth.login.ui;
    exports dev.webfx.stack.auth.login.ui.spi;

    // Used services
    uses dev.webfx.stack.auth.login.ui.spi.LoginUiProvider;

}