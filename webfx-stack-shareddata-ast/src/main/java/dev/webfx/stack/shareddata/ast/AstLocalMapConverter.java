package dev.webfx.stack.shareddata.ast;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstNode;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.shareddata.LocalMap;
import dev.webfx.stack.shareddata.impl.LocalMapConverter;

/**
 * @author Bruno Salmon
 */
public final class AstLocalMapConverter {

    public static LocalMap<String, Object> convertToAstLocalMap(LocalMap<String, String> localMap, String format) {
        return LocalMapConverter.convertLocalMap(localMap, text -> parseAst(text, format), o -> formatAst(o, format));
    }

    // For now, only Ast nodes and String are supported (other scalars such as numbers will be parsed as String)

    private static Object parseAst(String text, String format) {
        if (text != null) {
            ReadOnlyAstNode astNode = AST.parseNodeSilently(text, format);
            if (astNode != null)
                return astNode;
        }
        return text;
    }

    private static String formatAst(Object o, String format) {
        if (AST.isNode(o))
            return AST.formatNode((ReadOnlyAstNode) o, format);
        return Strings.toString(o);
    }

}
