package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.UsernamePasswordCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public class UsernamePasswordCredentialsSerialCodec extends SerialCodecBase<UsernamePasswordCredentials> {

    private static final String CODEC_ID = "UsernamePasswordCredentials";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

    public UsernamePasswordCredentialsSerialCodec() {
        super(UsernamePasswordCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(UsernamePasswordCredentials arg, AstObject serial) {
        encodeString(serial, USERNAME_KEY, arg.getUsername());
        encodeString(serial, PASSWORD_KEY, arg.getPassword());
    }

    @Override
    public UsernamePasswordCredentials decode(ReadOnlyAstObject serial) {
        return new UsernamePasswordCredentials(
                decodeString(serial, USERNAME_KEY),
                decodeString(serial, PASSWORD_KEY)
        );
    }
}
