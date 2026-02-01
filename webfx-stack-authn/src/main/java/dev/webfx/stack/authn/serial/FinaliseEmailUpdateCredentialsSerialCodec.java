package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.FinaliseEmailUpdateCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class FinaliseEmailUpdateCredentialsSerialCodec extends SerialCodecBase<FinaliseEmailUpdateCredentials> {

    private static final String CODEC_ID = "FinaliseEmailUpdateCredentials";
    private static final String TOKEN_KEY = "token";

    public FinaliseEmailUpdateCredentialsSerialCodec() {
        super(FinaliseEmailUpdateCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(FinaliseEmailUpdateCredentials arg, AstObject serial) {
        encodeString(serial, TOKEN_KEY, arg.magicLinkTokenOrVerificationCode());
    }

    @Override
    public FinaliseEmailUpdateCredentials decode(ReadOnlyAstObject serial) {
        return new FinaliseEmailUpdateCredentials(
            decodeString(serial, TOKEN_KEY)
        );
    }
}
