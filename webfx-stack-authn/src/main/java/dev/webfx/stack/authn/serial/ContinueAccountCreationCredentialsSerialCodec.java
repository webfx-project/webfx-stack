package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.ContinueAccountCreationCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class ContinueAccountCreationCredentialsSerialCodec extends SerialCodecBase<ContinueAccountCreationCredentials> {

    private static final String CODEC_ID = "ContinueAccountCreationCredentials";
    private static final String TOKEN_KEY = "token";

    public ContinueAccountCreationCredentialsSerialCodec() {
        super(ContinueAccountCreationCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(ContinueAccountCreationCredentials arg, AstObject serial) {
        encodeString(serial, TOKEN_KEY, arg.magicLinkTokenOrVerificationCode());
    }

    @Override
    public ContinueAccountCreationCredentials decode(ReadOnlyAstObject serial) {
        return new ContinueAccountCreationCredentials(
                decodeString(serial, TOKEN_KEY)
        );
    }
}
