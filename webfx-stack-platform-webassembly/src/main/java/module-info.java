// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.platform.webassembly {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;

    // Exported packages
    exports dev.webfx.stack.platform.webassembly;
    exports dev.webfx.stack.platform.webassembly.spi;

    // Used services
    uses dev.webfx.stack.platform.webassembly.spi.WebAssemblyProvider;

}