package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordMagicLinkCredentials {

    private final String newPassword;

    public UpdatePasswordMagicLinkCredentials(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
