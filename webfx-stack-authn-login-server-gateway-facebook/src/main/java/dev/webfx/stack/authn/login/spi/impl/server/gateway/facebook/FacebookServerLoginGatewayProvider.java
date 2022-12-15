package dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class FacebookServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String GATEWAY_ID = "Facebook";
    private final static String FACEBOOK_AUTH_PREFIX = "Facebook."; // Must match with FacebookServerAuthenticationGatewayProvider
    private static final String DIRECT_LOGIN_URL_TEMPLATE = "https://www.facebook.com/dialog/oauth?client_id={{CLIENT_ID}}&redirect_uri={{RETURN_URL}}?state={{SESSION_ID}}";
    private final static String JAVASCRIPT_SDK_LOGIN_HTML_TEMPLATE = "<!doctype html><html><body>\n" +
            "<script>\n" +
            "  window.fbAsyncInit = function() {\n" +
            "    FB.init({\n" +
            "      appId            : '{{CLIENT_ID}}',\n" +
            "      autoLogAppEvents : true,\n" +
            "      xfbml            : true,\n" +
            "      version          : 'v15.0'\n" +
            "    });\n" +
            "    FB.login(function(response){\n" +
            "       console.log('login response = ' + response); \n" +
            "    });" +
            "  };\n" +
            "</script>\n" +
            "<script async defer crossorigin=\"anonymous\" src=\"https://connect.facebook.net/en_US/sdk.js\"></script>" +
            "</body></html>";
    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";
    @Override
    public void boot() {
        if (isConfigurationValid()) {
            Router router = Router.create(); // Actually returns the http router (not creates a new one)

            // This login path is called by the web version only, when rendering the login page in an iFrame (see
            // getLoginUiInput() comments for more explanation). Please note that to make this work, the Facebook
            // developer account must have "Login with the JavaScript SDK" enabled, and your domain name needs to be
            // listed in "Allowed Domains for the JavaScript SDK". Also, the "App Mode" must be "Live" (it doesn't work
            // in "Development" mode).
            router.route(LOGIN_PATH).handler(rc -> {
                // The session id was passed as a query parameter (see getLoginUiInput()) and we get it back here.
                String serverSessionId = rc.getParams().getString("sessionId");
                // We return our FB JS SDK login html template, after resolving the variables
                // TODO: Finish the template once the "App Mode" is "Live" to make the final callback
                String loginHtml = resolveTemplateVariablesWithServerSessionId(JAVASCRIPT_SDK_LOGIN_HTML_TEMPLATE, serverSessionId);
                rc.sendResponse(loginHtml);
            });

            // Whatever the initial login page (either the direct Facebook url for the OpenJFX version, or the JavaScript
            // SDK page for the web version), a successful login will end up here, through a call to the redirect url.
            router.route(REDIRECT_PATH).handler(rc -> {
                // The user session id was passed using the Facebook "state" query parameter (only custom parameter allowed by FB)
                String sessionId = rc.getParams().get("state");
                // Facebook returns a code for the login success
                String code = rc.getParams().get("code");

                // We call AuthenticationService.authenticate() which expects that code for Facebook, and it will return
                // the final user id to be returned to the client. We make that call using ThreadLocalStateHolder with
                // a state that holds the session id, because this is how the FB gateway authenticate() method will
                // retrieve the session id. This session id is required by the FB gateway, only because it will fetch
                // a FB API url that requires to pass the exact same redirect uri as the one initially passed on the
                // login url (otherwise that FB API will fail), and that redirect uri contains the session id.
                ThreadLocalStateHolder.runWithState(StateAccessor.setServerSessionId(null, sessionId), () ->
                        AuthenticationService.authenticate(FACEBOOK_AUTH_PREFIX + code) // the Facebook prefix is required by the Authentication portal to dispatch it to the Facebook gateway
                        .compose(userId -> // On success, the FB gateway is returning the user id
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
                        .onSuccess(ignored -> sendHtmlResponse("Login successful", rc)) // on success, the UI router should anyway quickly replace the login web view with the page initially requested (if authorized)
                );
            });
        }
    }

    private static void sendHtmlResponse(String responseText, RoutingContext rc) {
        rc.sendResponse(HTML_RESPONSE.replace("{{RESPONSE_TEXT}}", responseText));
    }

    @Override
    public Object getGatewayId() {
        return GATEWAY_ID;
    }

    public Future<?> getLoginUiInput(Object loginUiContext) {
        // The UI counterpart is a web view, and when it calls this method, it passes a boolean value for the context
        // that tells if the web view is in an iFrame or not (true for the web version, false for the OpenJFX version).
        boolean isWebViewInIframe = Boolean.TRUE.equals(loginUiContext);
        // We also capture the server session id which will be passed in the login url.
        String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
        // After checking the configuration is valid (will return a failed Future if not), we return the Facebook login
        // url to be displayed for the web view (this url depends on whether the web view is in an iFrame).
        return checkConfigurationValid()
                .map(ignored ->
                    // If the web view is in an iFrame (web version), we can't use the direct Facebook url, because the
                    // iFrame will refuse to load from facebook.com, it will accept only an url from the same origin
                    // as the main browser url (i.e. your domain). So in that case, we must return the url that points to
                    // our own login path, which will be served by our router (see router.route(LOGIN_PATH).handler(...)
                    // in the boot() method above). Also, please note that the router will render a login page that uses
                    // the Facebook JavaScript SDK, because this is the only method provided by Facebook that works in
                    // an iFrame. Also note that this Facebook JavaScript SDK doesn't work in the OpenJFX web view for
                    // any reason. So for the OpenJFX version, we need to use the direct Facebook url instead.
                    isWebViewInIframe ? // is the web view in an iFrame ?
                            // If yes (web version), we return the url of the login page served by our own router (Facebook JavaScript SDK version)
                            LOGIN_ORIGIN + LOGIN_PATH + "?sessionId=" + serverSessionId
                            // If no (OpenJFX version), we return the direct Facebook url
                            : resolveTemplateVariablesWithServerSessionId(DIRECT_LOGIN_URL_TEMPLATE, serverSessionId)
                );
    }

    private String resolveTemplateVariables(String template) {
        return template
                .replace("{{CLIENT_ID}}", FACEBOOK_CLIENT_ID)
                .replace("{{CLIENT_SECRET}}", FACEBOOK_CLIENT_SECRET)
                .replace("{{RETURN_URL}}", REDIRECT_ORIGIN + REDIRECT_PATH)
                ;
    }

    private String resolveTemplateVariablesWithServerSessionId(String template, String serverSessionId) {
        return resolveTemplateVariables(template)
                .replace("{{SESSION_ID}}", serverSessionId);
    }

}
