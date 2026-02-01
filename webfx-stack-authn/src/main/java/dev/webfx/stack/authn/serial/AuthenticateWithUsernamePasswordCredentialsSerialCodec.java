package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.AuthenticateWithUsernamePasswordCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateWithUsernamePasswordCredentialsSerialCodec extends SerialCodecBase<AuthenticateWithUsernamePasswordCredentials> {

    private static final String CODEC_ID = "AuthenticateWithUsernamePasswordCredentials";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

    public AuthenticateWithUsernamePasswordCredentialsSerialCodec() {
        super(AuthenticateWithUsernamePasswordCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(AuthenticateWithUsernamePasswordCredentials arg, AstObject serial) {
        encodeString(serial, USERNAME_KEY, arg.username());
        encodeString(serial, PASSWORD_KEY, arg.password());
    }

    @Override
    public AuthenticateWithUsernamePasswordCredentials decode(ReadOnlyAstObject serial) {
        return new AuthenticateWithUsernamePasswordCredentials(
                decodeString(serial, USERNAME_KEY),
                decodeString(serial, PASSWORD_KEY)
        );
    }
}
