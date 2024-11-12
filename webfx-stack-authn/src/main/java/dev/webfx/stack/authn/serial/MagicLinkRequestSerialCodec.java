package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.MagicLinkRequest;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class MagicLinkRequestSerialCodec extends SerialCodecBase<MagicLinkRequest> {

    private static final String CODEC_ID = "MagicLinkRequest";
    private static final String EMAIL_KEY = "email";
    private static final String CLIENT_ORIGIN_KEY = "origin";
    private static final String REQUESTED_PATH_KEY = "path";
    private static final String LANGUAGE_KEY = "lang";
    private static final String CONTEXT_KEY = "context";

    public MagicLinkRequestSerialCodec() {
        super(MagicLinkRequest.class, CODEC_ID);
    }

    @Override
    public void encode(MagicLinkRequest arg, AstObject serial) {
        encodeString(serial, EMAIL_KEY,          arg.getEmail());
        encodeString(serial, CLIENT_ORIGIN_KEY,  arg.getClientOrigin());
        encodeString(serial, REQUESTED_PATH_KEY, arg.getRequestedPath());
        encodeObject(serial, LANGUAGE_KEY,       arg.getLanguage());
        encodeObject(serial, CONTEXT_KEY,        arg.getContext());
    }

    @Override
    public MagicLinkRequest decode(ReadOnlyAstObject serial) {
        return new MagicLinkRequest(
            decodeString(serial, EMAIL_KEY),
            decodeString(serial, CLIENT_ORIGIN_KEY),
            decodeString(serial, REQUESTED_PATH_KEY),
            decodeObject(serial, LANGUAGE_KEY),
            decodeObject(serial, CONTEXT_KEY)
        );
    }
}