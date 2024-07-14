package dev.webfx.stack.orm.entity;

/**
 * @author Bruno Salmon
 */
public final class EntityStoreQuery {

    private final String select;
    private final Object[] parameters;
    private final Object listId;

    public EntityStoreQuery(String select) {
        this(select, select);
    }

    public EntityStoreQuery(String select, Object listId) {
        this(select, null, listId);
    }

    public EntityStoreQuery(String select, Object[] parameters) {
        this(select, parameters, select);
    }

    public EntityStoreQuery(String select, Object[] parameters, Object listId) {
        this.select = select;
        this.parameters = parameters;
        this.listId = listId;
    }

    public String getSelect() {
        return select;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Object getListId() {
        return listId;
    }
}
