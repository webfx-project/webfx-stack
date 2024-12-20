package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.InitiateAccountCreationCredentials;

/**
 * @author Bruno Salmon
 */
public final class InitiateAccountCreationCredentialsSerialCodec extends AlternativeLoginActionCredentialsSerialCodec<InitiateAccountCreationCredentials> {

    private static final String CODEC_ID = "InitiateAccountCreationCredentials";

    public InitiateAccountCreationCredentialsSerialCodec() {
        super(InitiateAccountCreationCredentials.class, CODEC_ID);
    }

    @Override
    public InitiateAccountCreationCredentials decode(ReadOnlyAstObject serial) {
        return new InitiateAccountCreationCredentials(
            decodeString(serial, EMAIL_KEY),
            decodeString(serial, CLIENT_ORIGIN_KEY),
            decodeString(serial, REQUESTED_PATH_KEY),
            decodeObject(serial, LANGUAGE_KEY),
            decodeObject(serial, CONTEXT_KEY)
        );
    }
}