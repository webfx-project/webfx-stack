package dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public final class MojoAuthServerLoginGatewayConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private final static String MOJO_AUTH_LOGIN_CONFIGURATION_NAME = "MojoAuthLogin";
    private final static String API_KEY_CONF_KEY = "apiKey";
    private final static String REDIRECT_ORIGIN_CONF_KEY = "redirectOrigin";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    public static String MOJO_AUTH_API_KEY;
    static String REDIRECT_ORIGIN;
    static String REDIRECT_PATH;

    public MojoAuthServerLoginGatewayConfigurationConsumer() {
        super(MOJO_AUTH_LOGIN_CONFIGURATION_NAME, "MojoAuthLogin.default.json");
    }

    static boolean isConfigurationValid() {
        return ConfigurationService.areValuesNonNullAndResolved(MOJO_AUTH_API_KEY, REDIRECT_ORIGIN, REDIRECT_PATH);
    }

    static Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Invalid configuration for MojoAuth login");
    }

    @Override
    public Future<Void> boot(ReadOnlyKeyObject config) {
        if (config == null)
            return Future.failedFuture("No configuration found for MojoAuth login");
        MOJO_AUTH_API_KEY = config.getString(API_KEY_CONF_KEY);
        REDIRECT_ORIGIN = config.getString(REDIRECT_ORIGIN_CONF_KEY);
        REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);
        MojoAuthServerLoginGatewayCallbackListener.start();
        return checkConfigurationValid();
    }
}
