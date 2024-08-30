package dev.webfx.stack.i18n.spi.impl.ast;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.i18n.DefaultTokenKey;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.TokenKey;

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
        if (o instanceof ReadOnlyAstObject)
            return ((ReadOnlyAstObject) o).get(tokenKey.toString());
        return tokenKey == DefaultTokenKey.TEXT ? Strings.toString(o) : null;
    }
}
