// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.client.webassembly {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.platform.webassembly;
    exports dev.webfx.stack.platform.webassembly.spi;

    // Used services
    uses dev.webfx.stack.platform.webassembly.spi.WebAssemblyProvider;

}