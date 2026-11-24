package dev.webfx.stack.authn;

/**
 * Asks for the signed-in account's email to be changed: a link is sent to the new address, and following it
 * makes the change.
 *
 * <p>Carries the account's current password, because changing the sign-in email changes who can recover the
 * account: whoever controls the new address can then use "forgot password". A session alone proves who opened
 * it, not who holds it now, so the server may require the password (or another proof it accepts) before
 * sending anything. Null when the caller relies on such another proof.
 *
 * @author Bruno Salmon
 */
public final class InitiateEmailUpdateCredentials extends AlternativeLoginActionCredentials {

    private final String currentPassword;

    public InitiateEmailUpdateCredentials(String email, String clientOrigin, String requestedPath, Object language, boolean verificationCodeOnly, Object context) {
        this(email, clientOrigin, requestedPath, language, verificationCodeOnly, context, null);
    }

    public InitiateEmailUpdateCredentials(String email, String clientOrigin, String requestedPath, Object language, boolean verificationCodeOnly, Object context, String currentPassword) {
        super(email, clientOrigin, requestedPath, language, verificationCodeOnly, context);
        this.currentPassword = currentPassword;
    }

    /** The account's current password as typed, or null. */
    public String getCurrentPassword() {
        return currentPassword;
    }
}
