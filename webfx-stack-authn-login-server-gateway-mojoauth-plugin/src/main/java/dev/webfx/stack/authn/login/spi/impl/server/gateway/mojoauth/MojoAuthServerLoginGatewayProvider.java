package dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

/**
 * @author Bruno Salmon
 */
public class MojoAuthServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String CONFIG_PATH = "webfx.stack.authn.server.mojoauth";
    private final static String API_KEY_CONF_KEY = "apiKey";
    private final static String REDIRECT_ORIGIN_CONF_KEY = "redirectOrigin";
    private final static String REDIRECT_PATH_CONF_KEY = "redirectPath";

    public static String MOJO_AUTH_API_KEY;
    static String REDIRECT_ORIGIN;
    static String REDIRECT_PATH;

    private final static String GATEWAY_ID = "MojoAuth";
    private final static String MOJO_AUTH_PREFIX = "MojoAuth."; // Must match with MojoAuthServerAuthenticationGatewayProvider

    private static final String HTML_TEMPLATE = "<!DOCTYPE html>\n" +
            "<head>\n" +
            "    <script charset='UTF-8' src='https://cdn.mojoauth.com/js/mojoauth.min.js'>\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id='mojoauth-passwordless-form'></div>\n" +
            "<script>\n" +
            "\n" +
            "    const mojoauth = new MojoAuth('{{API_KEY}}', {\n" +
            "      language: 'en_GB',\n" +
            "      redirect_url: '{{RETURN_URL}}'," +
            "      source: [" +
            "       { type: 'email', feature: 'magiclink' } \n" +
//            "       { type: 'email', feature: 'otp' }, \n" +
//            "       { type: 'phone', feature: 'otp' } \n" +
            "       ]});\n" +
            "\n" +
            "    mojoauth.signIn().then(response => console.log(response));\n" +
            "\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>\n";

    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";

    @Override
    public void boot() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            if (config == null) {
                Console.log("❌ No configuration found for MojoAuth login (check " + CONFIG_PATH + ")");
                return;
            }

            MOJO_AUTH_API_KEY = config.getString(API_KEY_CONF_KEY);
            REDIRECT_ORIGIN = config.getString(REDIRECT_ORIGIN_CONF_KEY);
            REDIRECT_PATH = config.getString(REDIRECT_PATH_CONF_KEY);

            if (!isConfigurationValid()) {
                Console.log("❌ Invalid configuration for MojoAuth login (check " + CONFIG_PATH + ")");
                return;
            }
            Router router = Router.create(); // Actually returns the http router (not creates a new one)

            router.route(REDIRECT_PATH).handler(rc -> {
                AstObject params = rc.getParams();
                String stateId = params.getString("state_id");
                String sessionId = params.getString("sessionId");
                AuthenticationService.authenticate(MOJO_AUTH_PREFIX + stateId)
                        .compose(userId ->
                                SessionService.getSessionStore().get(sessionId)
                                        .map(session -> new Pair<>(userId, session)))
                        .compose(pair -> {
                            Session session = pair.get2();
                            String runId = SessionAccessor.getRunId(session);
                            if (runId == null)
                                return Future.failedFuture("Login push failed");
                            Object userId = pair.get1();
                            return PushServerService.pushState(StateAccessor.setUserId(null, userId), runId);
                        })
                        .onFailure(e -> sendHtmlResponse("Login failed: " + e.getMessage(), rc))
                        .onSuccess(ignored -> sendHtmlResponse("Login successful", rc));
            });
        });
    }

    public static boolean isConfigurationValid() {
        // Quick dirty test
        return MOJO_AUTH_API_KEY != null && !MOJO_AUTH_API_KEY.contains("${{");
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
        return checkConfigurationValid()
                .map(ignored -> {
                    String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
                    String RETURN_URL = REDIRECT_ORIGIN + REDIRECT_PATH;
                    String html = HTML_TEMPLATE
                            .replace("{{API_KEY}}", MOJO_AUTH_API_KEY)
                            .replace("{{RETURN_URL}}", RETURN_URL)
                            .replace(":sessionId", serverSessionId);
                    return html;
                });
    }

}
