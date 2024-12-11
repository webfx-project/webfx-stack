package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.FinaliseAccountCreationCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class FinaliseAccountCreationCredentialsSerialCodec extends SerialCodecBase<FinaliseAccountCreationCredentials> {

    private static final String CODEC_ID = "FinaliseAccountCreationCredentials";
    private static final String TOKEN_KEY = "token";
    private static final String PASSWORD_KEY = "password";

    public FinaliseAccountCreationCredentialsSerialCodec() {
        super(FinaliseAccountCreationCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(FinaliseAccountCreationCredentials arg, AstObject serial) {
        encodeString(serial, TOKEN_KEY, arg.getToken());
        encodeString(serial, PASSWORD_KEY, arg.getToken());
    }

    @Override
    public FinaliseAccountCreationCredentials decode(ReadOnlyAstObject serial) {
        return new FinaliseAccountCreationCredentials(
            decodeString(serial, TOKEN_KEY),
            decodeString(serial, PASSWORD_KEY)
        );
    }
}
