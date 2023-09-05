package dev.webfx.stack.conf.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public interface ConfigurationConsumer {

    default Future<Void> boot() { return Future.succeededFuture(); }

    String getConfigurationName();

    ReadOnlyAstObject getDefaultConfiguration();

}
