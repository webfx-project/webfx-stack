package dev.webfx.stack.auth.login.spi.impl.mojoauth;

import com.mojoauth.sdk.models.responsemodels.UserResponse;
import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.auth.authn.AuthenticationService;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.SessionAccessor;
import dev.webfx.stack.session.state.StateAccessor;

/**
 * @author Bruno Salmon
 */
public final class MojoAuthLoginModuleBooter implements ApplicationModuleBooter {

    private final static String HTML_RESPONSE = "<html><body style= \"width: 100%; height:62%; display: table; overflow: hidden;\">\n" +
            "    <p style=\"display: table-cell; text-align: center; vertical-align: middle;\">{{RESPONSE_TEXT}}</p>\n" +
            "</body></html>";

    @Override
    public String getModuleName() {
        return "webfx-stack-auth-login-server-mojoauth";
    }

    @Override
    public int getBootLevel() {
        return ApplicationModuleBooter.APPLICATION_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        Router router = Router.create();
        router.route(MojoAuthLoginServiceProvider.REDIRECT_PATH).handler(rc -> {
            String stateId = rc.getParams().getString("state_id");
            String sessionId = rc.getParams().getString("sessionId");
            System.out.println("state_id = " + stateId + ", requested sessionId = " + sessionId + ", vert.x sessionId = " + rc.session().id());
            AuthenticationService.authenticate(stateId)
                    .onComplete(ar -> {
                        String responseText = "Login failed";
                        if (ar.failed())
                            responseText = "Login error: " + ar.cause().getMessage();
                        else {
                            UserResponse userResponse = (UserResponse) ar.result();
                            if (userResponse.getAuthenticated()) {
                                String userId = userResponse.getUser().getUserId();
                                responseText = "Login successful! User id: " + userId;
                                SessionService.getSessionStore().get(sessionId)
                                        .onFailure(e -> e.printStackTrace())
                                        .onSuccess(session -> {
                                            String runId = SessionAccessor.getRunId(session);
                                            System.out.println("session.runId = " + runId);
                                            if (runId != null)
                                                PushServerService.pushState(StateAccessor.setUserId(null, userId), runId);
                                        });
                            }
                        }
                        rc.sendResponse(HTML_RESPONSE.replace("{{RESPONSE_TEXT}}", responseText));
                    });
        });

    }
}
