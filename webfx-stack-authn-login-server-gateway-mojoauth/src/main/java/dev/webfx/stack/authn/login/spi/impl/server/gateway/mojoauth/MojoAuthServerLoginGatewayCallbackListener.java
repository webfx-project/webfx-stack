package dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayConfigurationConsumer.REDIRECT_PATH;
import static dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayConfigurationConsumer.isConfigurationValid;

/**
 * @author Bruno Salmon
 */
final class MojoAuthServerLoginGatewayCallbackListener {

    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";

    static void start() {
        if (isConfigurationValid()) {
            Router router = Router.create(); // Actually returns the http router (not creates a new one)
            router.route("/login/callback/mojoAuth/sessionId/:sessionId").handler(rc -> {                               // @TODO extract this into constant
                String stateId = rc.getParams().getString("state_id");
                String sessionId = rc.getParams().getString("sessionId");
                Session webSession = rc.session();
                Console.log("state_id = " + stateId + ", requested sessionId = " + sessionId + ", webSessionId = " + webSession.id());

                AuthenticationService.authenticate(stateId)
                        .onComplete(ar -> {
                            String responseText;
                            if (ar.failed())
                                responseText = "Login error: " + ar.cause().getMessage();
                            else {
                                String oAuthAccessToken = ar.result().toString();
                                responseText = "Login successful";
                                // Retrieving runId from the session in order to push the user id to the client
                                SessionService.getSessionStore().get(sessionId)
                                        .onFailure(Throwable::printStackTrace)
                                        .onSuccess(session -> {
                                            String runId = SessionAccessor.getRunId(session);
                                            Console.log("session.runId = " + runId);
                                            if (runId != null)
                                                PushServerService.pushState(StateAccessor.setUserId(null, oAuthAccessToken), runId);
                                        });
                            }
                            rc.sendResponse(HTML_RESPONSE.replace("{{RESPONSE_TEXT}}", responseText));
                        });
            });
        }
    }
}
