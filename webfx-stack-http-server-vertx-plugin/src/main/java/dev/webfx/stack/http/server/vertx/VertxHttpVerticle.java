package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.vertx.common.VertxInstance;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;

import static dev.webfx.stack.http.server.vertx.VertxHttpModuleBooter.*;

/**
 * @author Bruno Salmon
 */
final class VertxHttpVerticle extends AbstractVerticle {

    @Override
    public void start() {
        consumeEachValidHttpServerConfiguration(httpServerConfig -> {
            String protocol = httpServerConfig.getString(PROTOCOL_CONFIG_KEY);
            int port = httpServerConfig.getInteger(PORT_CONFIG_KEY);
            String certPath = httpServerConfig.getString(CERT_PATH_CONFIG_KEY);
            String keyPath = httpServerConfig.getString(KEY_PATH_CONFIG_KEY);
            if (certPath == null && keyPath == null)
                createAndStartHttpServer(protocol, port, null);
            else
                createAndStartHttpServer(protocol, port, new PemKeyCertOptions().setCertPath(certPath).setKeyPath(keyPath));
        }, false); // No need to log invalid configuration again as it was already done
    }

    private void createAndStartHttpServer(String protocol, int port, PemKeyCertOptions pemKeyCertOptions) {
        //Console.log("Starting " + protocol + " server on port " + port);
        // Creating the http server with the following options:
        vertx.createHttpServer(new HttpServerOptions()
                .setMaxWebSocketFrameSize(65536 * 100) // Increasing the frame size to allow big client request
                .setCompressionSupported(true) // enabling gzip and deflate compression
                .setPort(port) // web port
                .setSsl(pemKeyCertOptions != null)
                .setKeyCertOptions(pemKeyCertOptions)
                .setUseAlpn(JdkSSLEngineOptions.isAlpnAvailable()) // Enabling http2 if ALPN package is available
        ) // Then plugging the http router
                .requestHandler(VertxInstance.getHttpRouter())
                // And finally starting the http server by listening the web port
                .listen()
                .onFailure(e -> Console.log("❌ Error while starting " + protocol + " server on port " + port, e))
                .onSuccess(x -> Console.log("✅ Successfully started " + protocol + " server on port " + port))
        ;
    }
}
