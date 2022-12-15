package dev.webfx.stack.authn.server.gateway.spi.impl;

import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.ReadOnlyJsonObject;

import java.util.Base64;

/**
 * @author Bruno Salmon
 */
public final class Jwt {

    private final String encodedHead;
    private final String encodedPayload;
    private final String encodedSignature;

    public Jwt(String token) {
        int firstDotIndex = token.indexOf('.');
        int secondDotIndex = token.indexOf('.', firstDotIndex + 1);
        encodedHead = token.substring(0, firstDotIndex);
        encodedPayload = token.substring(firstDotIndex + 1, secondDotIndex);
        encodedSignature = token.substring(secondDotIndex + 1);
    }

    public String getDecodedHead() {
        return decode64(encodedHead);
    }

    public String getDecodedPayload() {
        return decode64(encodedPayload);
    }

    public String getDecodedSignature() {
        return decode64(encodedSignature);
    }

    public ReadOnlyJsonObject getJsonHead() {
        return Json.parseObject(getDecodedHead());
    }

    public ReadOnlyJsonObject getJsonPayload() {
        return Json.parseObject(getDecodedPayload());
    }

    private String decode64(String encoded64) {
        return new String(Base64.getDecoder().decode(encoded64));
    }
}
