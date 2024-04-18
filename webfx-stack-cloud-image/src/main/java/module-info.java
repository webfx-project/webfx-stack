// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.image {

    // Direct dependencies modules
    requires transitive webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.fetch;
    requires webfx.platform.fetch.ast.json;
    requires webfx.platform.file;
    requires webfx.platform.util.http;

    // Exported packages
    exports dev.webfx.stack.cloud.image;
    exports dev.webfx.stack.cloud.image.impl.fetchbased;

}