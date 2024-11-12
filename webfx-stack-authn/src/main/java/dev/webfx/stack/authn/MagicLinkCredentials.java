package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class MagicLinkCredentials {

    private final String token;

    public MagicLinkCredentials(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
