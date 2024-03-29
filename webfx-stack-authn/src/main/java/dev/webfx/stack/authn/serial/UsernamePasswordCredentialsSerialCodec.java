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
    public void encodeToJson(UsernamePasswordCredentials arg, AstObject json) {
        json.set(USERNAME_KEY, arg.getUsername());
        json.set(PASSWORD_KEY, arg.getPassword());
    }

    @Override
    public UsernamePasswordCredentials decodeFromJson(ReadOnlyAstObject json) {
        return new UsernamePasswordCredentials(
                json.getString(USERNAME_KEY),
                json.getString(PASSWORD_KEY)
        );
    }
}
