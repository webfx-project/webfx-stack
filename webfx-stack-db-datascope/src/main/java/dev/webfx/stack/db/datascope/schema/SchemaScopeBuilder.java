package dev.webfx.stack.db.datascope.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class SchemaScopeBuilder {

    private final Map<Object /*classId*/, List<Object>> classFields = new HashMap<>();

    public SchemaScopeBuilder addClass(Object classId) {
        return addField(classId, null);
    }

    public SchemaScopeBuilder addField(Object classId, Object fieldId) {
        // A null fields list means "any field" (class-level scope, set via addClass).
        // containsKey distinguishes that marker from a simply absent class, so a
        // later addField can never silently downgrade a class-level scope to a
        // single field (which would make intersection miss modifications).
        if (fieldId == null) {
            classFields.put(classId, null);
            return this;
        }
        if (classFields.containsKey(classId)) {
            List<Object> fields = classFields.get(classId);
            if (fields != null) // null = class-level, already covers this field
                fields.add(fieldId);
        } else {
            List<Object> fields = new ArrayList<>();
            fields.add(fieldId);
            classFields.put(classId, fields);
        }
        return this;
    }

    public SchemaScope build() {
        return new SchemaScopeImpl(classFields.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SchemaScope.ClassScope(e.getKey(), e.getValue() == null ? null : e.getValue().toArray())
                )));
    }

}
