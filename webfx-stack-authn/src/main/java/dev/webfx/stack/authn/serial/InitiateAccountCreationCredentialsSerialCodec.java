package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.InitiateAccountCreationCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class InitiateAccountCreationCredentialsSerialCodec extends SerialCodecBase<InitiateAccountCreationCredentials> {

    private static final String CODEC_ID = "InitiateAccountCreationCredentials";
    private static final String EMAIL_KEY = "email";
    private static final String CLIENT_ORIGIN_KEY = "origin";
    private static final String LANGUAGE_KEY = "lang";
    private static final String CONTEXT_KEY = "context";

    public InitiateAccountCreationCredentialsSerialCodec() {
        super(InitiateAccountCreationCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(InitiateAccountCreationCredentials arg, AstObject serial) {
        encodeString(serial, EMAIL_KEY,          arg.getEmail());
        encodeString(serial, CLIENT_ORIGIN_KEY,  arg.getClientOrigin());
        encodeObject(serial, LANGUAGE_KEY,       arg.getLanguage());
        encodeObject(serial, CONTEXT_KEY,        arg.getContext());
    }

    @Override
    public InitiateAccountCreationCredentials decode(ReadOnlyAstObject serial) {
        return new InitiateAccountCreationCredentials(
            decodeString(serial, EMAIL_KEY),
            decodeString(serial, CLIENT_ORIGIN_KEY),
            decodeObject(serial, LANGUAGE_KEY),
            decodeObject(serial, CONTEXT_KEY)
        );
    }
}