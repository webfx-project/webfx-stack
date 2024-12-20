package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordFromMagicLinkCredentials {

    private final String newPassword;

    public UpdatePasswordFromMagicLinkCredentials(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
