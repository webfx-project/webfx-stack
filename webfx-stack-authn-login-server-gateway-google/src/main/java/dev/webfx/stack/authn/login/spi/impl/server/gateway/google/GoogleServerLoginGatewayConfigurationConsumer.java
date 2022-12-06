package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;

/**
 * @author Bruno Salmon
 */
public final class GoogleServerLoginGatewayConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private final static String GOOGLE_LOGIN_CONFIGURATION_NAME = "GoogleLogin";
    private final static String GOOGLE_CLIENT_ID_CONF_KEY = "googleClientId";
    private final static String GOOGLE_CLIENT_SECRET_CONF_KEY = "googleClientSecret";
    private final static String REDIRECT_HOST_CONF_KEY = "redirectHost";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    static String GOOGLE_CLIENT_ID;
    static String GOOGLE_CLIENT_SECRET;
    static String REDIRECT_HOST;
    static String REDIRECT_PATH;

    public GoogleServerLoginGatewayConfigurationConsumer() {
        super(GOOGLE_LOGIN_CONFIGURATION_NAME, "GoogleLogin.default.json");
    }

    static boolean isConfigurationValid() {
        return ConfigurationService.areValuesNonNullAndResolved(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, REDIRECT_HOST, REDIRECT_PATH);
    }

    static Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Invalid configuration for Google login");
    }

    @Override
    protected Future<Void> boot(ReadOnlyKeyObject config) {
        if (config == null)
            return Future.failedFuture("No configuration found for Google login");
        GOOGLE_CLIENT_ID = config.getString(GOOGLE_CLIENT_ID_CONF_KEY);
        GOOGLE_CLIENT_SECRET = config.getString(GOOGLE_CLIENT_SECRET_CONF_KEY);
        REDIRECT_HOST = config.getString(REDIRECT_HOST_CONF_KEY);
        REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);
        GoogleServerLoginGatewayCallbackListener.start();
        return checkConfigurationValid();
    }
}
