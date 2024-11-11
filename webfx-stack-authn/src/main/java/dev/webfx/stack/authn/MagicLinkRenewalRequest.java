package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class MagicLinkRenewalRequest {

    private final String previousToken;

    public MagicLinkRenewalRequest(String previousToken) {
        this.previousToken = previousToken;
    }

    public String getPreviousToken() {
        return previousToken;
    }
}
