package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UpdatePasswordFromMagicLinkCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordFromMagicLinkCredentialsSerialCodec extends SerialCodecBase<UpdatePasswordFromMagicLinkCredentials> {

    private static final String CODEC_ID = "UpdatePasswordFromMagicLinkCredentials";
    private static final String NEW_PASSWORD_KEY = "newPassword";

    public UpdatePasswordFromMagicLinkCredentialsSerialCodec() {
        super(UpdatePasswordFromMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(UpdatePasswordFromMagicLinkCredentials arg, AstObject serial) {
        encodeString(serial, NEW_PASSWORD_KEY, arg.getNewPassword());
    }

    @Override
    public UpdatePasswordFromMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new UpdatePasswordFromMagicLinkCredentials(
                decodeString(serial, NEW_PASSWORD_KEY)
        );
    }
}
