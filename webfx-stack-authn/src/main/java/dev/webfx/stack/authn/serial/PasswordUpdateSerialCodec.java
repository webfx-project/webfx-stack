package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.PasswordUpdate;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class PasswordUpdateSerialCodec extends SerialCodecBase<PasswordUpdate> {

    private static final String CODEC_ID = "PasswordUpdate";
    private static final String OLD_PASSWORD_KEY = "oldPassword";
    private static final String NEW_PASSWORD_KEY = "newPassword";

    public PasswordUpdateSerialCodec() {
        super(PasswordUpdate.class, CODEC_ID);
    }

    @Override
    public void encode(PasswordUpdate arg, AstObject serial) {
        encodeString(serial, OLD_PASSWORD_KEY, arg.getOldPassword());
        encodeString(serial, NEW_PASSWORD_KEY, arg.getNewPassword());
    }

    @Override
    public PasswordUpdate decode(ReadOnlyAstObject serial) {
        return new PasswordUpdate(
            decodeString(serial, OLD_PASSWORD_KEY),
            decodeString(serial, NEW_PASSWORD_KEY)
        );
    }
}
