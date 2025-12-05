package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.AuthenticateWithVerificationCodeCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateWithVerificationCodeCredentialsSerialCodec extends SerialCodecBase<AuthenticateWithVerificationCodeCredentials> {

    private static final String CODEC_ID = "AuthenticateWithVerificationCodeCredentials";
    private static final String VERIFICATION_CODE_KEY = "code";

    public AuthenticateWithVerificationCodeCredentialsSerialCodec() {
        super(AuthenticateWithVerificationCodeCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(AuthenticateWithVerificationCodeCredentials arg, AstObject serial) {
        encodeString(serial, VERIFICATION_CODE_KEY, arg.verificationCode());
    }

    @Override
    public AuthenticateWithVerificationCodeCredentials decode(ReadOnlyAstObject serial) {
        return new AuthenticateWithVerificationCodeCredentials(
                decodeString(serial, VERIFICATION_CODE_KEY)
        );
    }
}
