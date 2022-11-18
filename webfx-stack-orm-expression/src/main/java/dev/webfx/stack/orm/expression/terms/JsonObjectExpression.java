package dev.webfx.stack.orm.expression.terms;

import dev.webfx.extras.type.Type;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.noreflect.ReadOnlyIndexedArray;
import dev.webfx.platform.util.noreflect.ReadOnlyKeyObject;

// TODO: remove platform dependency

/**
 * @author Bruno Salmon
 */
public final class JsonObjectExpression<T> extends AbstractExpression<T> {

    private final ReadOnlyKeyObject jsonObjectExpressions;

    public JsonObjectExpression(ReadOnlyKeyObject jsonObjectExpressions) {
        super(1);
        this.jsonObjectExpressions = jsonObjectExpressions;
    }

    @Override
    public Type getType() {
        return null; // TODO JsonType?
    }

    @Override
    public ReadOnlyJsonObject evaluate(T domainObject, DomainReader<T> domainReader) {
        JsonObject json = Json.createObject();
        ReadOnlyIndexedArray keys = jsonObjectExpressions.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            Expression<T> expression = jsonObjectExpressions.get(key);
            json.set(key, expression.evaluate(domainObject, domainReader));
        }
        return json;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append('{');
        ReadOnlyIndexedArray keys = jsonObjectExpressions.keys();
        for (int i = 0; i < keys.size(); i++) {
            if (i != 0)
                sb.append(", ");
            String key = keys.getString(i);
            sb.append(key).append(": ");
            Expression<T> expression = jsonObjectExpressions.get(key);
            expression.toString(sb);
        }
        return sb.append('}');
    }

    @Override
    public void collect(CollectOptions options) {
        ReadOnlyIndexedArray keys = jsonObjectExpressions.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            Expression<T> expression = jsonObjectExpressions.get(key);
            expression.collect(options);
        }
    }
}
