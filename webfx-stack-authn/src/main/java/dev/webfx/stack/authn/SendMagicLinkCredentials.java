package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class SendMagicLinkCredentials extends AlternativeLoginActionCredentials {

    public SendMagicLinkCredentials(String email, String clientOrigin, String requestedPath, Object language, boolean verificationCodeOnly, Object context) {
        super(email, clientOrigin, requestedPath, language, verificationCodeOnly, context);
    }
}
