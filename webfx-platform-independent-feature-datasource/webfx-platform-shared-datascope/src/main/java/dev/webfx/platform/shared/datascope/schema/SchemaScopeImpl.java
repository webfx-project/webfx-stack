package dev.webfx.platform.shared.datascope.schema;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class SchemaScopeImpl implements SchemaScope {

    private final Map<Object /* classId */, ClassScope> classScopes;

    public SchemaScopeImpl(Map<Object, ClassScope> classScopes) {
        this.classScopes = classScopes;
    }

    @Override
    public ClassScope getClassScope(Object classId) {
        return classScopes.get(classId);
    }

    public boolean intersects(SchemaScope schemaScope) {
        if (true) // Temporary hack while the implementation doesn't work in all situations (ex: submit booking from front-end doesn't update bookings table)
            return true; // TODO fix the implementation to make it work in all situations
        for (ClassScope classScope1 : classScopes.values()) {
            ClassScope classScope2 = schemaScope.getClassScope(classScope1.classId);
            if (classScope2 != null && classScope1.intersects(classScope2))
                return true;
        }
        return false;
    }

}
