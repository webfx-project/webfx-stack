package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class MagicLinkRequest {

    private final String email;
    private final String clientOrigin;
    private final Object language;

    public MagicLinkRequest(String email, String clientOrigin, Object language) {
        this.email = email;
        this.clientOrigin = clientOrigin;
        this.language = language;
    }

    public String getEmail() {
        return email;
    }

    public String getClientOrigin() {
        return clientOrigin;
    }

    public Object getLanguage() {
        return language;
    }
}
