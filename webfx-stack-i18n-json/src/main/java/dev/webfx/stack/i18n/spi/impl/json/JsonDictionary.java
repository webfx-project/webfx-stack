package dev.webfx.stack.i18n.spi.impl.json;

import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.i18n.DefaultTokenKey;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.TokenKey;

/**
 * @author Bruno Salmon
 */
final class JsonDictionary implements Dictionary {

    private final ReadOnlyJsonObject json;

    JsonDictionary(ReadOnlyJsonObject json) {
        this.json = json;
    }

    JsonDictionary(String json) {
        this(Json.parseObject(json));
    }

    @Override
    public <TK extends Enum<?> & TokenKey> Object getMessageTokenValue(Object messageKey, TK tokenKey) {
        String jsonKey = Strings.toString(messageKey);
        Object o = json.get(jsonKey);
        if (o instanceof ReadOnlyJsonObject)
            return ((ReadOnlyJsonObject) o).get(tokenKey.name().toLowerCase());
        return tokenKey == DefaultTokenKey.TEXT ? Strings.toString(o) : null;
    }
}
