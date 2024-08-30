package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.MagicLinkCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class MagicLinkCredentialsSerialCodec extends SerialCodecBase<MagicLinkCredentials> {

    private static final String CODEC_ID = "MagicLinkCredentials";
    private static final String TOKEN_KEY = "token";

    public MagicLinkCredentialsSerialCodec() {
        super(MagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(MagicLinkCredentials arg, AstObject serial) {
        encodeString(serial, TOKEN_KEY, arg.getToken());
    }

    @Override
    public MagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new MagicLinkCredentials(
                decodeString(serial, TOKEN_KEY)
        );
    }
}
