package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UpdatePasswordMagicLinkCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordMagicLinkCredentialsSerialCodec extends SerialCodecBase<UpdatePasswordMagicLinkCredentials> {

    private static final String CODEC_ID = "UpdatePasswordMagicLinkCredentials";
    private static final String NEW_PASSWORD_KEY = "newPassword";

    public UpdatePasswordMagicLinkCredentialsSerialCodec() {
        super(UpdatePasswordMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(UpdatePasswordMagicLinkCredentials arg, AstObject serial) {
        encodeString(serial, NEW_PASSWORD_KEY, arg.getNewPassword());
    }

    @Override
    public UpdatePasswordMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new UpdatePasswordMagicLinkCredentials(
                decodeString(serial, NEW_PASSWORD_KEY)
        );
    }
}
