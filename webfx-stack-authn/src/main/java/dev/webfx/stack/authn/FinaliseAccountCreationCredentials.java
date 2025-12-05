package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public record FinaliseAccountCreationCredentials(String magicLinkTokenOrVerificationCode, String password) {

}
