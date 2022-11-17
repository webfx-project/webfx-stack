package dev.webfx.stack.auth.authn;

/**
 * @author Bruno Salmon
 */
public final class AuthenticationArgument {

    private final Object identity;
    private final Object credentials;

    public AuthenticationArgument(Object identity, Object credentials) {
        this.identity = identity;
        this.credentials = credentials;
    }

    public Object getIdentity() {
        return identity;
    }

    public Object getCredentials() {
        return credentials;
    }
}
