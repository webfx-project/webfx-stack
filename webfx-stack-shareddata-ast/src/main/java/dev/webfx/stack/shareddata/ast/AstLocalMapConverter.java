package dev.webfx.stack.shareddata.ast;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.shareddata.LocalMap;
import dev.webfx.stack.shareddata.impl.LocalMapConverter;

/**
 * @author Bruno Salmon
 */
public final class AstLocalMapConverter {

    public static LocalMap<String, Object> convertToAstLocalMap(LocalMap<String, String> localMap, String format) {
        return LocalMapConverter.convertLocalMap(localMap, text -> parseAst(text, format), o -> formatAst(o, format));
    }

    // TODO: add scalar and array support (not only AST objects)

    private static Object parseAst(String text, String format) {
        if (text != null)
            return AST.parseObject(text, format);
        return null;
    }

    private static String formatAst(Object o, String format) {
        if (AST.isObject(o))
            return AST.formatObject((ReadOnlyAstObject) o, format);
        return null;
    }

}
