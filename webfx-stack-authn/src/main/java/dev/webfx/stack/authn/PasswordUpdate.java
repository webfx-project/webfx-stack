package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public class PasswordUpdate {

    private final String oldPassword;
    private final String newPassword;

    public PasswordUpdate(String oldPassword, String newPassword) {
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
