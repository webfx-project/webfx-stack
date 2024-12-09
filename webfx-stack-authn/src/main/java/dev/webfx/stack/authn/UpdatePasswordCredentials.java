package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordCredentials {

    private final String oldPassword;
    private final String newPassword;

    public UpdatePasswordCredentials(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
