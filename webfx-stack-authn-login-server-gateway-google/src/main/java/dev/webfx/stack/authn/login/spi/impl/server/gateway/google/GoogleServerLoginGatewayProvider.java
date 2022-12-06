package dev.webfx.stack.authn.login.spi.impl.server.gateway.google;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.google.GoogleServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class GoogleServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String GATEWAY_ID = "Google";
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

    @Override
    public Object getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public Future<?> getLoginUiInput() {
        return checkConfigurationValid()
                .map(ignored -> {
                    String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
                    String RETURN_URL = "http://localhost:8080" + REDIRECT_PATH;                    // @TODO implement better solution here
                    String html = HTML_TEMPLATE
                            .replace("{{GOOGLE_CLIENT_ID}}", GOOGLE_CLIENT_ID)
                            .replace("{{RETURN_URL}}", RETURN_URL)
                            .replace("{{SESSION_ID}}", serverSessionId);
                    return html;
                });
    }

}
