package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class UserClaimsSerialCodec extends SerialCodecBase<UserClaims> {

    private static final String CODEC_ID = "UserClaims";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String PHONE_KEY = "phone";
    private static final String OTHER_CLAIMS_KEY = "otherClaims";

    public UserClaimsSerialCodec() {
        super(UserClaims.class, CODEC_ID);
    }

    @Override
    public void encode(UserClaims arg, AstObject serial) {
        encodeString(serial, USERNAME_KEY,     arg.getUsername());
        encodeString(serial, EMAIL_KEY,        arg.getEmail());
        encodeString(serial, PHONE_KEY,        arg.getPhone());
        encodeObject(serial, OTHER_CLAIMS_KEY, arg.getOtherClaims());
    }

    @Override
    public UserClaims decode(ReadOnlyAstObject serial) {
        return new UserClaims(
                decodeString(serial, USERNAME_KEY),
                decodeString(serial, EMAIL_KEY),
                decodeString(serial, PHONE_KEY),
                decodeObject(serial, OTHER_CLAIMS_KEY)
        );
    }
}
