package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class InitiateAccountCreationCredentials extends AlternativeLoginActionCredentials {

    public InitiateAccountCreationCredentials(String email, String clientOrigin, String requestedPath, Object language, Object context) {
        super(email, clientOrigin, requestedPath, language, context);
    }
}

