package dev.webfx.stack.platform.webassembly;

import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface WebAssemblyModule {

    Future<WebAssemblyInstance> instantiate(WebAssemblyImport... imports);

}
