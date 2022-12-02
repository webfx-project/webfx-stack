package dev.webfx.stack.authn.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.ReadOnlyJsonObject;
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
    public void encodeToJson(UsernamePasswordCredentials arg, JsonObject json) {
        json.set(USERNAME_KEY, arg.getUsername());
        json.set(PASSWORD_KEY, arg.getPassword());
    }

    @Override
    public UsernamePasswordCredentials decodeFromJson(ReadOnlyJsonObject json) {
        return new UsernamePasswordCredentials(
                json.getString(USERNAME_KEY),
                json.getString(PASSWORD_KEY)
        );
    }
}
