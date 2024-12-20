package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class RenewMagicLinkCredentials {

    private final String previousToken;

    public RenewMagicLinkCredentials(String previousToken) {
        this.previousToken = previousToken;
    }

    public String getPreviousToken() {
        return previousToken;
    }
}
