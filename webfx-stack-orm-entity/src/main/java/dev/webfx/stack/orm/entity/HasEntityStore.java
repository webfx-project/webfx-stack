package dev.webfx.stack.orm.entity;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.HasDataSourceModel;

/**
 * @author Bruno Salmon
 */
public interface HasEntityStore extends HasDataSourceModel {

    EntityStore getStore();

    @Override
    default DataSourceModel getDataSourceModel() {
        return getStore().getDataSourceModel();
    }
}
