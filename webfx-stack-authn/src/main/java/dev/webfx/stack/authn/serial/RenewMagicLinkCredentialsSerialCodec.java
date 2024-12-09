package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.RenewMagicLinkCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class RenewMagicLinkCredentialsSerialCodec extends SerialCodecBase<RenewMagicLinkCredentials> {

    private static final String CODEC_ID = "RenewMagicLinkCredentials";
    private static final String PREVIOUS_TOKEN_KEY = "previousToken";

    public RenewMagicLinkCredentialsSerialCodec() {
        super(RenewMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(RenewMagicLinkCredentials arg, AstObject serial) {
        encodeString(serial, PREVIOUS_TOKEN_KEY, arg.getPreviousToken());
    }

    @Override
    public RenewMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new RenewMagicLinkCredentials(
            decodeString(serial, PREVIOUS_TOKEN_KEY)
        );
    }
}