package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.MagicLinkRenewalRequest;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class MagicLinkRenewalRequestSerialCodec extends SerialCodecBase<MagicLinkRenewalRequest> {

    private static final String CODEC_ID = "MagicLinkRenewalRequest";
    private static final String PREVIOUS_TOKEN_KEY = "previousToken";

    public MagicLinkRenewalRequestSerialCodec() {
        super(MagicLinkRenewalRequest.class, CODEC_ID);
    }

    @Override
    public void encode(MagicLinkRenewalRequest arg, AstObject serial) {
        encodeString(serial, PREVIOUS_TOKEN_KEY, arg.getPreviousToken());
    }

    @Override
    public MagicLinkRenewalRequest decode(ReadOnlyAstObject serial) {
        return new MagicLinkRenewalRequest(
            decodeString(serial, PREVIOUS_TOKEN_KEY)
        );
    }
}