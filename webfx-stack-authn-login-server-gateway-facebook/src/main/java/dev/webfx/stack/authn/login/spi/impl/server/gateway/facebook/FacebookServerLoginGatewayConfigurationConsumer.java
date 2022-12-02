package dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public final class FacebookServerLoginGatewayConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private final static String FACEBOOK_LOGIN_CONF_NAME = "FacebookLogin";
    private final static String FACEBOOK_CLIENT_ID_CONF_KEY = "facebookClientId";
    private final static String REDIRECT_HOST_CONF_KEY = "redirectHost";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    static String FACEBOOK_CLIENT_ID;
    static String REDIRECT_HOST;
    static String REDIRECT_PATH;


    public FacebookServerLoginGatewayConfigurationConsumer() {
        super(FACEBOOK_LOGIN_CONF_NAME, "FacebookLogin.default.json");
    }

    static boolean isConfigurationValid() {
        return ConfigurationService.areValuesNonNullAndResolved(FACEBOOK_CLIENT_ID, REDIRECT_HOST, REDIRECT_PATH);
    }

    static Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Invalid configuration for Facebook login");
    }

    @Override
    public Future<Void> boot(ReadOnlyKeyObject config) {
        if (config == null)
            return Future.failedFuture("No configuration found for Facebook login");
        FACEBOOK_CLIENT_ID = config.getString(FACEBOOK_CLIENT_ID_CONF_KEY);
        REDIRECT_HOST = config.getString(REDIRECT_HOST_CONF_KEY);
        REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);
        FacebookServerLoginGatewayCallbackListener.start();
        return checkConfigurationValid();
    }
}
