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
    private final static String MOJO_AUTH_API_KEY_CONF_KEY = "mojoAuthApiKey";
    private final static String REDIRECT_HOST_CONF_KEY = "redirectHost";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    static String MOJO_AUTH_API_KEY;
    static String REDIRECT_HOST;
    static String REDIRECT_PATH;

    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";

    public MojoAuthServerLoginGatewayConfigurationConsumer() {
        super(MOJO_AUTH_LOGIN_CONFIGURATION_NAME, "MojoAuthLogin.default.json");
    }

    static boolean isConfigurationValid() {
        return ConfigurationService.areValuesNonNullAndResolved(MOJO_AUTH_API_KEY, REDIRECT_HOST, REDIRECT_PATH);
    }

    static Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Invalid configuration for MojoAuth login");
    }

    @Override
    public Future<Void> boot(ReadOnlyKeyObject config) {
        if (config == null)
            return Future.failedFuture("No configuration found for MojoAuth login");
        MOJO_AUTH_API_KEY = config.getString(MOJO_AUTH_API_KEY_CONF_KEY);
        REDIRECT_HOST = config.getString(REDIRECT_HOST_CONF_KEY);
        REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);
        MojoAuthServerLoginGatewayCallbackListener.start();
        return checkConfigurationValid();
    }
}
