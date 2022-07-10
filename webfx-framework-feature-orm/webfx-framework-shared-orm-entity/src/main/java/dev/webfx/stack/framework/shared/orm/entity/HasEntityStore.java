package dev.webfx.stack.framework.shared.orm.entity;

import dev.webfx.stack.framework.shared.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.framework.shared.orm.domainmodel.HasDataSourceModel;

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
