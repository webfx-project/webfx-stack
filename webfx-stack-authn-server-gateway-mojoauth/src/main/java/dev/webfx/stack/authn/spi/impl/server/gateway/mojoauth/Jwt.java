package dev.webfx.stack.authn.spi.impl.server.gateway.mojoauth;

import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.ReadOnlyJsonObject;

import java.util.Base64;

/**
 * @author Bruno Salmon
 */
final class Jwt {

    private final String encodedHead;
    private final String encodedPayload;
    private final String encodedSignature;

    Jwt(String token) {
        int firstDotIndex = token.indexOf('.');
        int secondDotIndex = token.indexOf('.', firstDotIndex + 1);
        encodedHead = token.substring(0, firstDotIndex);
        encodedPayload = token.substring(firstDotIndex + 1, secondDotIndex);
        encodedSignature = token.substring(secondDotIndex + 1);
    }

    String getDecodedHead() {
        return decode64(encodedHead);
    }

    String getDecodedPayload() {
        return decode64(encodedPayload);
    }

    String getDecodedSignature() {
        return decode64(encodedSignature);
    }

    ReadOnlyJsonObject getJsonHead() {
        return Json.parseObject(getDecodedHead());
    }

    ReadOnlyJsonObject getJsonPayload() {
        return Json.parseObject(getDecodedPayload());
    }

    private String decode64(String encoded64) {
        return new String(Base64.getDecoder().decode(encoded64));
    }
}
