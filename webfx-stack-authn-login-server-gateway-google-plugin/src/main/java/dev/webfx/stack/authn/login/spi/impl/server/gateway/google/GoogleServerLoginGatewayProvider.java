package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.authn.oauth2.OAuth2Service;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

/**
 * @author Bruno Salmon
 */
public class GoogleServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String CONFIG_PATH = "webfx.stack.authn.server.google";
    private final static String CLIENT_ID_CONF_KEY = "clientId";
    private final static String CLIENT_SECRET_CONF_KEY = "clientSecret";
    private final static String LOGIN_ORIGIN_CONF_KEY = "loginOrigin";
    private final static String LOGIN_PATH_CONF_KEY = "loginPath";
    private final static String REDIRECT_ORIGIN_CONF_KEY = "redirectOrigin";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    static String GOOGLE_CLIENT_ID;
    static String GOOGLE_CLIENT_SECRET;
    static String LOGIN_ORIGIN;
    static String LOGIN_PATH;
    static String REDIRECT_ORIGIN;
    static String REDIRECT_PATH;

    private final static String GATEWAY_ID = "Google";
    private final static String GOOGLE_AUTH_PREFIX = "Google."; // Must match with GoogleServerAuthenticationGatewayProvider
    private final static String OAUTH2_GOOGLE_SITE = "https://accounts.google.com";

    private static final String HTML_TEMPLATE = "<!doctype html><html>\n" +
            "<body>\n" +
            "<script src=\"https://accounts.google.com/gsi/client\" async defer></script>\n" +
            "<div id=\"g_id_onload\"\n" +
            "     data-client_id=\"{{GOOGLE_CLIENT_ID}}\"\n" +
            "     data-login_uri=\"{{RETURN_URL}}?sessionId={{SESSION_ID}}\"\n" +
            "     data-auto_prompt=\"false\">\n" +
            "</div>\n" +
            "<div class=\"g_id_signin\"\n" +
            "     data-type=\"standard\"\n" +
            "     data-size=\"large\"\n" +
            "     data-theme=\"outline\"\n" +
            "     data-text=\"sign_in_with\"\n" +
            "     data-shape=\"rectangular\"\n" +
            "     data-logo_alignment=\"left\">\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>\n";

    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";

    @Override
    public void boot() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            if (config == null) {
                Console.log("❌ No configuration found for Google login (check " + CONFIG_PATH + ")");
                return;
            }

            GOOGLE_CLIENT_ID = config.getString(CLIENT_ID_CONF_KEY);
            GOOGLE_CLIENT_SECRET = config.getString(CLIENT_SECRET_CONF_KEY);
            LOGIN_ORIGIN = config.getString(LOGIN_ORIGIN_CONF_KEY);
            LOGIN_PATH = config.getString(LOGIN_PATH_CONF_KEY);
            REDIRECT_ORIGIN = config.getString(REDIRECT_ORIGIN_CONF_KEY);
            REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);

            if (!isConfigurationValid()) {
                Console.log("❌ Invalid configuration for Google login (check " + CONFIG_PATH + ")");
                return;
            }

            Router router = Router.create(); // Actually returns the http router (not creates a new one)

            // Initialising OAuth2 for Google:
            OAuth2Service.discover(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, OAUTH2_GOOGLE_SITE)
                    .onFailure(e -> Console.log("❌ Error while initialising Google OAuth2: " + e.getMessage()))
                    .onSuccess(oauth2 -> { // OAuth2 is now ready, and Google tokens will now be automatically be decoded and verified.
                        // Success login redirect
                        router.route(REDIRECT_PATH).handler(rc -> {
                            String sessionId = rc.getParams().get("sessionId");
                            String credential = rc.getParams().get("credential");

                            AuthenticationService.authenticate(GOOGLE_AUTH_PREFIX + credential) // the Google prefix is required by the Authentication portal to dispatch it to the Google gateway
                                    .compose(userId -> // On success, the Google gateway is returning the user id
                                            // We will push that user id to the client, but to do that, we need the client runId
                                            // which is stored in the session. So we first load the session:
                                            SessionService.getSessionStore().get(sessionId)
                                                    // Once done, we push the user id to the client using the state
                                                    .compose(session -> PushServerService.pushState(
                                                            StateAccessor.setUserId(null, userId), // we create a state that holds the user id
                                                            SessionAccessor.getRunId(session)) // and push it to the runId stored in the session
                                                    )
                                    ) // Finally we return the final content to be displayed in the web view
                                    .onFailure(e -> sendHtmlResponse("Login failed with message: " + e.getMessage(), rc))
                                    .onSuccess(ignored -> sendHtmlResponse("Login successful", rc)); // on success, the UI router should anyway quickly replace the login web view with the page initially requested (if authorized)
                        });
                        Console.log("✅ Successfully initialised Google OAuth2");
                    });

            // Google login page (will be displayed in the web view on the client)
            router.route(LOGIN_PATH).handler(rc -> {
                String serverSessionId = rc.getParams().getString("sessionId");
                String RETURN_URL = REDIRECT_ORIGIN + REDIRECT_PATH;
                String html = HTML_TEMPLATE
                        .replace("{{GOOGLE_CLIENT_ID}}", GOOGLE_CLIENT_ID)
                        .replace("{{RETURN_URL}}", RETURN_URL)
                        .replace("{{SESSION_ID}}", serverSessionId);
                //Console.log("iFrame content: " + html);
                rc.sendResponse(html);
            });
        });
    }

    public static boolean isConfigurationValid() {
        // Quick dirty test
        return GOOGLE_CLIENT_ID != null && !GOOGLE_CLIENT_ID.contains("${{");
    }

    private Future<Void> checkConfigurationValid() {
        return isConfigurationValid() ? Future.succeededFuture() : Future.failedFuture("Configuration invalid");
    }

    private static void sendHtmlResponse(String responseText, RoutingContext rc) {
        rc.sendResponse(HTML_RESPONSE.replace("{{RESPONSE_TEXT}}", responseText));
    }

    @Override
    public Object getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public Future<?> getLoginUiInput(Object gatewayContext) {
        String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
        return checkConfigurationValid()
                .map(ignored -> {
                    String loginUrl = LOGIN_ORIGIN + LOGIN_PATH + "?sessionId=" + serverSessionId;
                    //Console.log("Login url = " + loginUrl);
                    return loginUrl;
                });
    }

}
