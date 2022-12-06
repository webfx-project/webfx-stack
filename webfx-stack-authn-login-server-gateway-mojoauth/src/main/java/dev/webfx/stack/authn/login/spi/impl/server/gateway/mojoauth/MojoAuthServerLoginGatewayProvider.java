package dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class MojoAuthServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String GATEWAY_ID = "MojoAuth";
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
            "      redirect_url: '{{RETURN_URL}}/sessionId/{{SESSION_ID}}'," +
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

    @Override
    public Object getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public Future<?> getLoginUiInput() {
        return checkConfigurationValid()
                .map(ignored -> {
                    String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
                    String RETURN_URL = "http://localhost:" + REDIRECT_HOST + REDIRECT_PATH;        // @TODO implement better solution here; Note that Facebook can't redirect to localhost
                    String html = HTML_TEMPLATE
                            .replace("{{API_KEY}}", MOJO_AUTH_API_KEY)
                            .replace("{{RETURN_URL}}", RETURN_URL)
                            .replace("{{SESSION_ID}}", serverSessionId);
                    return html;
                });
    }

}
