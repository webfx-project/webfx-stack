package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class UserClaimsSerialCodec extends SerialCodecBase<UserClaims> {

    private static final String CODEC_ID = "UserClaims";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String PHONE_KEY = "phone";
    private static final String OTHER_CLAIMS_KEY = "otherClaims";

    public UserClaimsSerialCodec() {
        super(UserClaims.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(UserClaims arg, AstObject json) {
        json.set(USERNAME_KEY, arg.getUsername());
        json.set(EMAIL_KEY, arg.getEmail());
        json.set(PHONE_KEY, arg.getPhone());
        json.setObject(OTHER_CLAIMS_KEY, arg.getOtherClaims());
    }

    @Override
    public UserClaims decodeFromJson(ReadOnlyAstObject json) {
        return new UserClaims(
                json.getString(USERNAME_KEY),
                json.getString(EMAIL_KEY),
                json.getString(PHONE_KEY),
                json.getObject(OTHER_CLAIMS_KEY)
        );
    }
}
