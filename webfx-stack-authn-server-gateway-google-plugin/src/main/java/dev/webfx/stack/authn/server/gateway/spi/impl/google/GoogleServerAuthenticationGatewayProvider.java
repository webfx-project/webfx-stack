package dev.webfx.stack.authn.server.gateway.spi.impl.google;

import dev.webfx.platform.async.Promise;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.impl.Jwt;
import dev.webfx.stack.authn.server.gateway.spi.impl.ServerAuthenticationGatewayProviderBase;

/**
 * @author Bruno Salmon
 */
public final class GoogleServerAuthenticationGatewayProvider extends ServerAuthenticationGatewayProviderBase {

    private final static String GOOGLE_GATEWAY_AUTH_PREFIX = "Google.";

    public GoogleServerAuthenticationGatewayProvider() {
        super(GOOGLE_GATEWAY_AUTH_PREFIX);
    }

    @Override
    protected void authenticateImpl(String token, Promise<String> promise) {
        promise.complete(GOOGLE_GATEWAY_AUTH_PREFIX + token); // No double verification for now
    }

    @Override
    protected void getUserClaimsImpl(String token, Promise<UserClaims> promise) {
        Jwt jwt = new Jwt(token);
        ReadOnlyJsonObject jsonPayload = jwt.getJsonPayload();
        promise.complete(new UserClaims(
                jsonPayload.getString("name"),
                jsonPayload.getString("email"),
                jsonPayload.getString("phone"),
                jsonPayload
        ));
    }
}
