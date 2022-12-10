package dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public final class FacebookServerLoginGatewayConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private final static String LOGIN_CONF_NAME = "FacebookLogin";
    private final static String CLIENT_ID_CONF_KEY = "clientId";
    private final static String CLIENT_SECRET_CONF_KEY = "clientSecret";
    private final static String REDIRECT_ORIGIN_CONF_KEY = "redirectOrigin";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    static String FACEBOOK_CLIENT_ID;
    static String FACEBOOK_CLIENT_SECRET;
    static String REDIRECT_ORIGIN;
    static String REDIRECT_PATH;


    public FacebookServerLoginGatewayConfigurationConsumer() {
        super(LOGIN_CONF_NAME, "FacebookLogin.default.json");
    }

    static boolean isConfigurationValid() {
        return ConfigurationService.areValuesNonNullAndResolved(FACEBOOK_CLIENT_ID, FACEBOOK_CLIENT_SECRET, REDIRECT_ORIGIN, REDIRECT_PATH);
    }

    static Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Invalid configuration for Facebook login");
    }

    @Override
    public Future<Void> boot(ReadOnlyKeyObject config) {
        if (config == null)
            return Future.failedFuture("No configuration found for Facebook login");
        FACEBOOK_CLIENT_ID = config.getString(CLIENT_ID_CONF_KEY);
        FACEBOOK_CLIENT_SECRET = config.getString(CLIENT_SECRET_CONF_KEY);
        REDIRECT_ORIGIN = config.getString(REDIRECT_ORIGIN_CONF_KEY);
        REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);
        FacebookServerLoginGatewayCallbackListener.start();
        return checkConfigurationValid();
    }
}
