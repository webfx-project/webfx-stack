package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.authn.oauth2.OAuth2Service;
import dev.webfx.stack.routing.router.Router;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.google.GoogleServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
final class GoogleServerLoginGatewayCallbackListener {

    static void start() {
        if (isConfigurationValid()) {
            OAuth2Service.discover(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, "https://accounts.google.com")
                .onFailure(Console::log)
                .onSuccess(oauth2 -> {
                    // Auth is now ready to use, and google signature keys are loaded so tokens can be decoded and verified.

                    // Capture success redirects from Google SSO
                    Router router = Router.create(); // Actually returns the http router (not creates a new one)
                    router.route(REDIRECT_PATH).handler(rc -> {

                        String sessionId = rc.getParams().get("sessionId");
                        String credential = rc.getParams().get("credential");

                        Console.log("Google callback with credential=" + credential + ", and sessionId=" + sessionId);

                        rc.sendResponse("OK");
                    });
                });
        }
    }

 /*
    protected void processSuccess(String sessionId, String credential, OAuth2Auth oauth2) {
        System.out.println("Received SUCCESS response from Google");

        // Extract information from the JWT provided by Google
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String[] chunks = credential.split("\\.");
        String header = new String(decoder.decode(chunks[0]));
        JsonObject payload = new JsonObject(new String(decoder.decode(chunks[1])));
        String name = (String) payload.getValue("name");
        String email = (String) payload.getValue("email");

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
*/
}

