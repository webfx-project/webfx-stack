package dev.webfx.stack.framework.shared.orm.expression.builder.terms;

import dev.webfx.stack.framework.shared.orm.expression.terms.JsonObjectExpression;
import dev.webfx.platform.shared.util.noreflect.IndexedArray;
import dev.webfx.stack.platform.json.Json;
import dev.webfx.stack.platform.json.WritableJsonObject;

/**
 * @author Bruno Salmon
 */
public final class JsonObjectExpressionBuilder extends ExpressionBuilder {

    private final WritableJsonObject jsonObjectExpressionBuilders = Json.createObject();

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
            WritableJsonObject jsonExpressions = Json.createObject();
            IndexedArray keys = jsonObjectExpressionBuilders.keys();
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
