package dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.routing.router.Router;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayConfigurationConsumer.REDIRECT_PATH;
import static dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayConfigurationConsumer.isConfigurationValid;

/**
 * @author Bruno Salmon
 */
final class FacebookServerLoginGatewayCallbackListener {

    static void start() {
        if (isConfigurationValid()) {
            Router router = Router.create(); // Actually returns the http router (not creates a new one)
            router.route(REDIRECT_PATH).handler(rc -> {
                Console.log("Facebook callback!!!");
            });
        }
    }
}
