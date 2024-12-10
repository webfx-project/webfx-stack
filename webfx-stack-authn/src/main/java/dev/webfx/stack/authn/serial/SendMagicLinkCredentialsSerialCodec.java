package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.SendMagicLinkCredentials;

/**
 * @author Bruno Salmon
 */
public final class SendMagicLinkCredentialsSerialCodec extends AlternativeLoginActionCredentialsSerialCodec<SendMagicLinkCredentials> {

    private static final String CODEC_ID = "SendMagicLinkCredentials";

    public SendMagicLinkCredentialsSerialCodec() {
        super(SendMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public SendMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new SendMagicLinkCredentials(
            decodeString(serial, EMAIL_KEY),
            decodeString(serial, CLIENT_ORIGIN_KEY),
            decodeString(serial, REQUESTED_PATH_KEY),
            decodeObject(serial, LANGUAGE_KEY),
            decodeObject(serial, CONTEXT_KEY)
        );
    }
}