package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.routing.router.Router;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth;
import io.vertx.ext.auth.oauth2.*;
import java.util.Base64;
import io.vertx.core.json.JsonObject;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.google.GoogleServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
final class GoogleServerLoginGatewayCallbackListener {

    static void start() {
        if (isConfigurationValid()) {
            Router router = Router.create(); // Actually returns the http router (not creates a new one)

            /*
            router.route(REDIRECT_PATH).handler(rc -> {
                Console.log("Google callback 2!!!");
            });
            */

            OpenIDConnectAuth.discover(
                Vertx.vertx(),
                new OAuth2Options()
                    .setClientId(GOOGLE_CLIENT_ID)
                    .setClientSecret(GOOGLE_CLIENT_SECRET)
                    .setSite("https://accounts.google.com"))
                .onSuccess(oauth2 -> {
                    // Auth is now ready to use and google signature keys are loaded so tokens can be decoded and verified.

                    // Capture success redirects from Google SSO
                    router.route(REDIRECT_PATH).handler(rc -> {

                        String sessionId = rc.getParams().get("sessionId");
                        String credential = rc.getParams().get("credential");

                        System.out.println(sessionId);
                        System.out.println(credential);

                    /*
                    String sessionId = rc.request().getParam("session_id"); // The session request ID from the URL
                    String credential = rc.request().getParam("credential"); // Get the JWT provided by Google

                    if (sessionId != null && credential != null) {
                        processSuccess(sessionId, credential, oauth2);
                    } else {
                        processFailure();
                    }

                    HttpServerResponse response = rc.response();
                    response.end();

                     */
                    });
                })
                .onFailure(err -> {
                    // the setup failed.
                });

        }
    }

    protected void processSuccess(String sessionId, String credential, OAuth2Auth oauth2) {
        System.out.println("Received SUCCESS response from Google");

        // Extract information from the JWT provided by Google
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = credential.split("\\.");
        String header = new String(decoder.decode(chunks[0]));
        JsonObject payload = new JsonObject(new String(decoder.decode(chunks[1])));
        String name = (String)payload.getValue("name");
        String email = (String)payload.getValue("email");

        System.out.println("Session id: " + sessionId);
        System.out.println("Credential : " + credential);
        System.out.println(name + " " + email);

        // @TODO
        // Verify the JWT from Google using the oauth2 object passed in

        // If all is correct, update the sesssion database
        // sessionLogger.persist(sessionId, SessionLogger.SESSION_CONFIRMED, payload);

        // @TODO
        // Push a logged-in-success notification to the client"
    }

    protected void processFailure() {
        System.out.println("Failed to process sessionId and credential from Google");
    }
}
