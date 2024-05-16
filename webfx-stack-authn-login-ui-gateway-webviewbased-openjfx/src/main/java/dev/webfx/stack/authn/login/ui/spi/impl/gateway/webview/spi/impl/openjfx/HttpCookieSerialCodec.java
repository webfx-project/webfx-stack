package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.net.HttpCookie;

/**
 * @author Bruno Salmon
 */
public class HttpCookieSerialCodec extends SerialCodecBase<HttpCookie> {

    private final static String CODEC_ID = "HttpCookie";
    private static final String NAME_KEY = "name";
    private static final String VALUE_KEY = "value";
    private static final String SECURE_KEY = "secure";
    private static final String DOMAIN_KEY = "domain";
    private static final String EXPIRES_KEY = "expires";
    private static final String PATH_KEY = "path";
    private static final String HTTP_ONLY_KEY = "httpOnly";
    private static final String PORT_LIST_KEY = "portList";
    private static final String DISCARD_KEY = "discard";
    private static final String VERSION_KEY = "version";
    private static final String COMMENT_KEY = "comment";
    private static final String COMMENT_URL_KEY = "commentUrl";

    public HttpCookieSerialCodec() {
        super(HttpCookie.class, CODEC_ID);
    }

    @Override
    public void encode(HttpCookie c, AstObject serial) {
        // We need to know the cookie creation time to serialize the expiration time (see below). HttpCookie has a
        // whenCreated field for that, but we can't use it as it's a private & inaccessible field. Instead, we use an
        // alternative solution provided by FXLoginCookieStore that keeps an internal record of the cookie creation date.
        long whenCreated = FXLoginCookieStore.getWhenCreated(c); // cookie creation time in millis
        encodeString( serial, NAME_KEY,       c.getName(), NullEncoding.NULL_VALUE_NOT_ALLOWED);
        encodeString( serial, VALUE_KEY,      c.getValue());
        encodeBoolean(serial, SECURE_KEY,     c.getSecure());
        encodeString( serial, DOMAIN_KEY,     c.getDomain());
        // We don't serialize maxAge because it's relative. We serialize the absolute expiration time instead.
        encodeLong(   serial, EXPIRES_KEY, whenCreated + 1000L * c.getMaxAge()); // maxAge is in seconds, so x1000 to get millis
        encodeString( serial, PATH_KEY,        c.getPath());
        encodeBoolean(serial, HTTP_ONLY_KEY,   c.isHttpOnly());
        encodeString( serial, PORT_LIST_KEY,   c.getPortlist());
        encodeBoolean(serial, DISCARD_KEY,     c.getDiscard());
        encodeInteger(serial, VERSION_KEY,     c.getVersion());
        encodeString( serial, COMMENT_KEY,     c.getComment());
        encodeString( serial, COMMENT_URL_KEY, c.getCommentURL());
    }

    @Override
    public HttpCookie decode(ReadOnlyAstObject serial) {
        HttpCookie cookie = new HttpCookie(
                decodeString(serial, NAME_KEY, NullEncoding.NULL_VALUE_NOT_ALLOWED),
                decodeString(serial, VALUE_KEY));
        long whenCreated = FXLoginCookieStore.getWhenCreated(cookie); // will return 'now', as it's a new cookie
        cookie.setSecure(    decodeBoolean(serial, SECURE_KEY));
        cookie.setDomain(    decodeString( serial, DOMAIN_KEY));
        // We recalculate maxAge, which is the difference between the expiration and the creation time
        cookie.setMaxAge((   decodeLong(   serial, EXPIRES_KEY) - whenCreated) / 1000); // maxAge is in seconds, so /1000
        cookie.setPath(      decodeString( serial, PATH_KEY));
        cookie.setHttpOnly(  decodeBoolean(serial, HTTP_ONLY_KEY));
        cookie.setPortlist(  decodeString( serial, PORT_LIST_KEY));
        cookie.setDiscard(   decodeBoolean(serial, DISCARD_KEY));
        cookie.setVersion(   decodeInteger(serial, VERSION_KEY));
        cookie.setComment(   decodeString( serial, COMMENT_KEY));
        cookie.setCommentURL(decodeString( serial, COMMENT_URL_KEY));
        return cookie;
    }
}
