package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.InitiateEmailUpdateCredentials;

/**
 * @author Bruno Salmon
 */
public final class InitiateEmailUpdateCredentialsSerialCodec extends AlternativeLoginActionCredentialsSerialCodec<InitiateEmailUpdateCredentials> {

    private static final String CODEC_ID = "InitiateEmailUpdateCredentials";

    public InitiateEmailUpdateCredentialsSerialCodec() {
        super(InitiateEmailUpdateCredentials.class, CODEC_ID);
    }

    @Override
    public InitiateEmailUpdateCredentials decode(ReadOnlyAstObject serial) {
        return new InitiateEmailUpdateCredentials(
            decodeString(serial,  EMAIL_KEY),
            decodeString(serial,  CLIENT_ORIGIN_KEY),
            decodeString(serial,  REQUESTED_PATH_KEY),
            decodeObject(serial,  LANGUAGE_KEY),
            decodeBoolean(serial, VERIFICATION_CODE_ONLY_KEY),
            decodeObject(serial,  CONTEXT_KEY)
        );
    }
}