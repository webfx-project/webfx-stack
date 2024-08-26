package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public class MagicLinkPasswordUpdate {

    private String newPassword;

    public MagicLinkPasswordUpdate(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
