package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx;

import dev.webfx.platform.ast.json.JsonObject;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
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
    public void encodeToJson(HttpCookie c, JsonObject json) {
        // We need to know the cookie creation time to serialize the expiration time (see below). HttpCookie has a
        // whenCreated field for that, but we can't use it as it's a private & inaccessible field. Instead, we use an
        // alternative solution provided by FXLoginCookieStore that keeps an internal record of the cookie creation date.
        long whenCreated = FXLoginCookieStore.getWhenCreated(c); // cookie creation time in millis
        json
                .set(NAME_KEY, c.getName())
                .set(VALUE_KEY, c.getValue())
                .set(SECURE_KEY, c.getSecure())
                .set(DOMAIN_KEY, c.getDomain())
                // We don't serialize maxAge because it's relative. We serialize the absolute expiration time instead.
                .set(EXPIRES_KEY, whenCreated + 1000L * c.getMaxAge()) // maxAge is in seconds, so x1000 to get millis
                .set(PATH_KEY, c.getPath())
                .set(HTTP_ONLY_KEY, c.isHttpOnly())
                .set(PORT_LIST_KEY, c.getPortlist())
                .set(DISCARD_KEY, c.getDiscard())
                .set(VERSION_KEY, c.getVersion())
                .set(COMMENT_KEY, c.getComment())
                .set(COMMENT_URL_KEY, c.getCommentURL())
        ;
    }

    @Override
    public HttpCookie decodeFromJson(ReadOnlyJsonObject json) {
        HttpCookie cookie = new HttpCookie(json.getString(NAME_KEY), json.getString(VALUE_KEY));
        long whenCreated = FXLoginCookieStore.getWhenCreated(cookie); // will return 'now', as it's a new cookie
        cookie.setSecure(json.getBoolean(SECURE_KEY));
        cookie.setDomain(json.getString(DOMAIN_KEY));
        // We recalculate maxAge, which is the difference between the expiration and the creation time
        cookie.setMaxAge((json.getLong(EXPIRES_KEY) - whenCreated) / 1000); // maxAge is in seconds, so /1000
        cookie.setPath(json.getString(PATH_KEY));
        cookie.setHttpOnly(json.getBoolean(HTTP_ONLY_KEY));
        cookie.setPortlist(json.getString(PORT_LIST_KEY));
        cookie.setDiscard(json.getBoolean(DISCARD_KEY));
        cookie.setVersion(json.getInteger(VERSION_KEY));
        cookie.setComment(json.getString(COMMENT_KEY));
        cookie.setCommentURL(json.getString(COMMENT_URL_KEY));
        return cookie;
    }
}
