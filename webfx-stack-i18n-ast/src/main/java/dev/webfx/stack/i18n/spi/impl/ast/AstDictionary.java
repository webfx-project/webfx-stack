package dev.webfx.stack.i18n.spi.impl.ast;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.i18n.DefaultTokenKey;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.TokenKey;
import dev.webfx.stack.i18n.spi.impl.I18nProviderImpl;

/**
 * @author Bruno Salmon
 */
final class AstDictionary implements Dictionary {

    private final ReadOnlyAstObject dictionary;

    AstDictionary(ReadOnlyAstObject dictionary) {
        this.dictionary = dictionary;
    }

    AstDictionary(String text, String format) {
        this(AST.parseObject(text, format));
    }

    @Override
    public <TK extends Enum<?> & TokenKey> Object getMessageTokenValue(Object messageKey, TK tokenKey, boolean ignoreCase) {
        String key = Strings.toString(messageKey);
        Object o = dictionary.get(key);
        if (o == null && ignoreCase) {
            for (Object k : dictionary.keys()) {
                String sk = Strings.toString(k);
                if (key.equalsIgnoreCase(sk)) {
                    o = dictionary.get(sk);
                    break;
                }
            }
        }
        Object value;
        // If we have a multi-token message (coming from json, yaml, etc...), we return the value corresponding to the
        // requested token
        if (o instanceof ReadOnlyAstObject) {
            value = ((ReadOnlyAstObject) o).get(tokenKey.toString());
            // If the value itself is again an object (which can happen with graphic defined as an svgPath with
            // associated properties such as fill, stroke, etc...), we extend the i18n interpretation features (normally
            // designed for simple text values) to this object too (ex: fill = [brandMainColor])
            if (value instanceof AstObject) {
                interpretBracketsAndDefaultInAstObjectValues((AstObject) value, messageKey);
            }
        } else
            value = tokenKey == DefaultTokenKey.TEXT ? Strings.toString(o) : null;
        return value;
    }

    private void interpretBracketsAndDefaultInAstObjectValues(AstObject o, Object messageKey) {
        ReadOnlyAstArray keys = o.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getElement(i);
            Object value = o.get(key);
            if (value instanceof ReadOnlyAstObject)
                interpretBracketsAndDefaultInAstObjectValues((AstObject) value, messageKey);
            else if (value instanceof String) {
                Object newTokenValue = ((I18nProviderImpl) I18n.getProvider()).interpretBracketsAndDefaultInTokenValue(value, messageKey, "", DefaultTokenKey.TEXT, this, false, this, true);
                o.set(key, newTokenValue);
            }
        }
    }
}
