package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public abstract class AlternativeLoginActionCredentials {

    private final String email;
    private final String clientOrigin; // ex: https://mydomain.com The magic link will start with the same origin, so it goes back to the same server
    private final String requestedPath;
    private final Object language;
    private final boolean verificationCodeOnly; // request to use verification code only (not magic link) to prevent losing the application context (code entered within the same page)
    private final Object context; // ex: ModalityContext with organization and event => used to select the correct mailbox and magic link letter template

    public AlternativeLoginActionCredentials(String email, String clientOrigin, String requestedPath, Object language, boolean verificationCodeOnly, Object context) {
        this.email = email;
        this.clientOrigin = clientOrigin;
        this.requestedPath = requestedPath;
        this.language = language;
        this.verificationCodeOnly = verificationCodeOnly;
        this.context = context;
    }

    public String getEmail() {
        return email;
    }

    public String getClientOrigin() {
        return clientOrigin;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public Object getLanguage() {
        return language;
    }

    public boolean isVerificationCodeOnly() {
        return verificationCodeOnly;
    }

    public Object getContext() {
        return context;
    }

}
