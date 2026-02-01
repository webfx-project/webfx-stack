package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.AuthenticateWithMagicLinkCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateWithMagicLinkCredentialsSerialCodec extends SerialCodecBase<AuthenticateWithMagicLinkCredentials> {

    private static final String CODEC_ID = "AuthenticateWithMagicLinkCredentials";
    private static final String TOKEN_KEY = "token";

    public AuthenticateWithMagicLinkCredentialsSerialCodec() {
        super(AuthenticateWithMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public void encode(AuthenticateWithMagicLinkCredentials arg, AstObject serial) {
        encodeString(serial, TOKEN_KEY, arg.token());
    }

    @Override
    public AuthenticateWithMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new AuthenticateWithMagicLinkCredentials(
                decodeString(serial, TOKEN_KEY)
        );
    }
}
