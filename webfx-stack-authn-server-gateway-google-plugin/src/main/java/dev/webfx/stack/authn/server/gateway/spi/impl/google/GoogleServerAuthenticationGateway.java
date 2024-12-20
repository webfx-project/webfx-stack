package dev.webfx.stack.authn.server.gateway.spi.impl.google;

import dev.webfx.platform.async.Promise;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.impl.Jwt;
import dev.webfx.stack.authn.server.gateway.spi.impl.ServerAuthenticationGatewayBase;

/**
 * @author Bruno Salmon
 */
public final class GoogleServerAuthenticationGateway extends ServerAuthenticationGatewayBase {

    private final static String GOOGLE_GATEWAY_AUTH_PREFIX = "Google.";

    public GoogleServerAuthenticationGateway() {
        super(GOOGLE_GATEWAY_AUTH_PREFIX);
    }

    @Override
    protected void authenticateImpl(String token, Promise<String> promise) {
        promise.complete(GOOGLE_GATEWAY_AUTH_PREFIX + token); // No double verification for now
    }

    @Override
    protected void getUserClaimsImpl(String token, Promise<UserClaims> promise) {
        Jwt jwt = new Jwt(token);
        ReadOnlyAstObject jsonPayload = jwt.getJsonPayload();
        promise.complete(new UserClaims(
                jsonPayload.getString("name"),
                jsonPayload.getString("email"),
                jsonPayload.getString("phone"),
                jsonPayload
        ));
    }
}
