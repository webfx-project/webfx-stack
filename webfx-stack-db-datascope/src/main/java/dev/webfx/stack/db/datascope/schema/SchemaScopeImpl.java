package dev.webfx.stack.db.datascope.schema;

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
        // Note: statement-derived schema scopes can't see side effects happening
        // OUTSIDE the statement (KBS2-side writes, which never pulse this server
        // anyway). PostgreSQL trigger cascades within a submit are covered by the
        // partition dimension (AggregateScope) that the DQL interceptors now
        // derive: a cascade stays inside the same partition (event/booking) as
        // the statement that fired it.
        for (ClassScope classScope1 : classScopes.values()) {
            ClassScope classScope2 = schemaScope.getClassScope(classScope1.classId);
            if (classScope2 != null && classScope1.intersects(classScope2))
                return true;
        }
        return false;
    }

}
