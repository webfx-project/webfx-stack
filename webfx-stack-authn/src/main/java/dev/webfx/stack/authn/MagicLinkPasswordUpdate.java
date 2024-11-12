package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class MagicLinkPasswordUpdate {

    private final String newPassword;

    public MagicLinkPasswordUpdate(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
