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
    public <TK extends Enum<?> & TokenKey> Object getMessageTokenValue(Object messageKey, TK tokenKey) {
        String key = Strings.toString(messageKey);
        Object o = dictionary.get(key);
        Object value;
        if (o instanceof ReadOnlyAstObject) {
            value = ((ReadOnlyAstObject) o).get(tokenKey.toString());
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
