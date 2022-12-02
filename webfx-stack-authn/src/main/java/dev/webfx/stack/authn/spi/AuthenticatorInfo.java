package dev.webfx.stack.authn.spi;

/**
 * @author Bruno Salmon
 */
public class AuthenticatorInfo {

    private final Object authenticatorId;
    private final Object authenticatorI18nKey;

    public AuthenticatorInfo(Object authenticatorId, Object authenticatorI18nKey) {
        this.authenticatorId = authenticatorId;
        this.authenticatorI18nKey = authenticatorI18nKey;
    }

    public Object getAuthenticatorId() {
        return authenticatorId;
    }

    public Object getAuthenticatorI18nKey() {
        return authenticatorI18nKey;
    }
}
