package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.stack.orm.expression.terms.JsonObjectExpression;
import dev.webfx.platform.util.keyobject.ReadOnlyIndexedArray;
import dev.webfx.platform.json.Json;

/**
 * @author Bruno Salmon
 */
public final class JsonObjectExpressionBuilder extends ExpressionBuilder {

    private final JsonObject jsonObjectExpressionBuilders = Json.createObject();

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
            JsonObject jsonExpressions = Json.createObject();
            ReadOnlyIndexedArray keys = jsonObjectExpressionBuilders.keys();
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
