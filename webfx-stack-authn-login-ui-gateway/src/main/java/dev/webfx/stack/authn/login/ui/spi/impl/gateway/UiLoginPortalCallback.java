package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

/**
 * @author Bruno Salmon
 */
public interface UiLoginPortalCallback {

    void notifyInitializationFailure();

    void notifyUserLoginSuccessful();

    void notifyUserLoginFailed();

}
