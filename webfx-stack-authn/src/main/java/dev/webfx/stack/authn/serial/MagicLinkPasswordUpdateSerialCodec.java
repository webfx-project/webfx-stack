package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.MagicLinkPasswordUpdate;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class MagicLinkPasswordUpdateSerialCodec extends SerialCodecBase<MagicLinkPasswordUpdate> {

    private static final String CODEC_ID = "MagicLinkPasswordUpdate";
    private static final String NEW_PASSWORD_KEY = "newPassword";

    public MagicLinkPasswordUpdateSerialCodec() {
        super(MagicLinkPasswordUpdate.class, CODEC_ID);
    }

    @Override
    public void encode(MagicLinkPasswordUpdate arg, AstObject serial) {
        encodeString(serial, NEW_PASSWORD_KEY, arg.getNewPassword());
    }

    @Override
    public MagicLinkPasswordUpdate decode(ReadOnlyAstObject serial) {
        return new MagicLinkPasswordUpdate(
                decodeString(serial, NEW_PASSWORD_KEY)
        );
    }
}
