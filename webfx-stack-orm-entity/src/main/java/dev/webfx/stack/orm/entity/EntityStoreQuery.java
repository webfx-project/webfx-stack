package dev.webfx.stack.orm.entity;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class EntityStoreQuery {

    private final String select;
    private final Object listId;
    private final Object[] parameters;
    private final String[] parameterNames;

    public EntityStoreQuery(String select, Object... parameters) {
        this(select, null, parameters); // entity list will not be memorized in the entity store if not listId is provided
    }

    public EntityStoreQuery(String select, Object listId, Object[] parameters) {
        this.select = select;
        this.listId = listId;
        // If entities are passed as parameters, they are replaced by their primary keys to prepare their serialization
        // to the server over the network or to the client cache (entities themselves are not serializable)
        String[][] parameterNamesHolder = { null };
        this.parameters = DqlQueries.resolveParameters(parameters, paramNames -> parameterNamesHolder[0] = paramNames);
        this.parameterNames = parameterNamesHolder[0];
    }

    public EntityStoreQuery(String select, Object listId, Object[] parameters, String[] parameterNames) {
        this.select = select;
        this.listId = listId;
        this.parameters = parameters;
        this.parameterNames = parameterNames;
    }

    public String getSelect() {
        return select;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public Object getListId() {
        return listId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EntityStoreQuery that = (EntityStoreQuery) o;
        return select.equals(that.select) && dev.webfx.platform.util.Objects.areEquals(parameters, that.parameters, true) && Objects.equals(listId, that.listId);
    }

    @Override
    public int hashCode() {
        int result = select.hashCode();
        result = 31 * result + Arrays.hashCode(parameters);
        result = 31 * result + Objects.hashCode(listId);
        return result;
    }
}
