package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UpdatePasswordCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class UpdatePasswordCredentialsSerialCodec extends SerialCodecBase<UpdatePasswordCredentials> {

    private static final String CODEC_ID = "UpdatePasswordCredentials";
    private static final String OLD_PASSWORD_KEY = "oldPassword";
    private static final String NEW_PASSWORD_KEY = "newPassword";

    public UpdatePasswordCredentialsSerialCodec() {
        super(UpdatePasswordCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(UpdatePasswordCredentials arg, AstObject serial) {
        encodeString(serial, OLD_PASSWORD_KEY, arg.oldPassword());
        encodeString(serial, NEW_PASSWORD_KEY, arg.newPassword());
    }

    @Override
    public UpdatePasswordCredentials decode(ReadOnlyAstObject serial) {
        return new UpdatePasswordCredentials(
            decodeString(serial, OLD_PASSWORD_KEY),
            decodeString(serial, NEW_PASSWORD_KEY)
        );
    }
}
