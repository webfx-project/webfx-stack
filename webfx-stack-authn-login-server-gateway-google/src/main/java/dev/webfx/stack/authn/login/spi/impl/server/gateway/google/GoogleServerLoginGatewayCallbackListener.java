package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.routing.router.Router;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.google.GoogleServerLoginGatewayConfigurationConsumer.REDIRECT_PATH;
import static dev.webfx.stack.authn.login.spi.impl.server.gateway.google.GoogleServerLoginGatewayConfigurationConsumer.isConfigurationValid;

/**
 * @author Bruno Salmon
 */
final class GoogleServerLoginGatewayCallbackListener {

    static void start() {
        if (isConfigurationValid()) {
            Router router = Router.create(); // Actually returns the http router (not creates a new one)
            router.route(REDIRECT_PATH).handler(rc -> {
                Console.log("Google callback!!!");
            });
        }
    }
}
