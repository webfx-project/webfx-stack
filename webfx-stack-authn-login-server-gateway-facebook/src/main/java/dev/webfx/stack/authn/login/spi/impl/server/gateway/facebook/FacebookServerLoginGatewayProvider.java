package dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.ServerLoginGatewayProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGatewayConfigurationConsumer.*;

/**
 * @author Bruno Salmon
 */
public class FacebookServerLoginGatewayProvider implements ServerLoginGatewayProvider {

    private final static String GATEWAY_ID = "Facebook";
    private static final String HTML_TEMPLATE = "https://www.facebook.com/dialog/oauth?client_id={{FACEBOOK_CLIENT_ID}}&redirect_uri={{RETURN_URL}}";

    @Override
    public Object getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public Future<?> getLoginUiInput() {
        return checkConfigurationValid()
                .map(ignored -> {
                    String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
                    String RETURN_URL = REDIRECT_HOST + REDIRECT_PATH;
                    String html = HTML_TEMPLATE
                            .replace("{{FACEBOOK_CLIENT_ID}}", FACEBOOK_CLIENT_ID)
                            .replace("{{RETURN_URL}}", RETURN_URL)
                            .replace("{{SESSION_ID}}", serverSessionId);
                    return html;
                });
    }

}
