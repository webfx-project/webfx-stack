package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.authn.AlternativeLoginActionCredentials;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public abstract class AlternativeLoginActionCredentialsSerialCodec<T extends AlternativeLoginActionCredentials> extends SerialCodecBase<T> {

    protected static final String EMAIL_KEY = "email";
    protected static final String CLIENT_ORIGIN_KEY = "origin";
    protected static final String REQUESTED_PATH_KEY = "path";
    protected static final String LANGUAGE_KEY = "lang";
    protected static final String CONTEXT_KEY = "context";

    public AlternativeLoginActionCredentialsSerialCodec(Class<? extends T> javaClass, String codecId) {
        super(javaClass, codecId);
    }

    @Override
    public void encode(T arg, AstObject serial) {
        encodeString(serial, EMAIL_KEY,          arg.getEmail());
        encodeString(serial, CLIENT_ORIGIN_KEY,  arg.getClientOrigin());
        encodeString(serial, REQUESTED_PATH_KEY, arg.getRequestedPath());
        encodeObject(serial, LANGUAGE_KEY,       arg.getLanguage());
        encodeObject(serial, CONTEXT_KEY,        arg.getContext());
    }

}