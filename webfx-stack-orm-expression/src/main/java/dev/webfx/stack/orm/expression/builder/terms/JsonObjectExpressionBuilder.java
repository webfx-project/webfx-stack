package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.stack.orm.expression.terms.JsonObjectExpression;

/**
 * @author Bruno Salmon
 */
public final class JsonObjectExpressionBuilder extends ExpressionBuilder {

    private final AstObject jsonObjectExpressionBuilders = AST.createObject();

    private JsonObjectExpression jsonObjectExpression;

    public JsonObjectExpressionBuilder() {
    }

    public JsonObjectExpressionBuilder(String key, ExpressionBuilder eb) {
        add(key, eb);
    }

    public JsonObjectExpressionBuilder add(String key, ExpressionBuilder eb) {
        jsonObjectExpressionBuilders.set(key, eb);
        return this;
    }

    public JsonObjectExpression build() {
        if (jsonObjectExpression == null) {
            propagateDomainClasses();
            AstObject jsonExpressions = AST.createObject();
            ReadOnlyAstArray keys = jsonObjectExpressionBuilders.keys();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.getString(i);
                ExpressionBuilder expression = jsonObjectExpressionBuilders.get(key);
                expression.buildingClass = buildingClass;
                jsonExpressions.set(key, expression.build());
            }
            jsonObjectExpression = new JsonObjectExpression(jsonExpressions);
        }
        return jsonObjectExpression;
    }
}
