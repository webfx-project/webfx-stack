package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateWithMagicLinkCredentials {

    private final String token;

    public AuthenticateWithMagicLinkCredentials(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
