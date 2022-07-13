package dev.webfx.stack.orm.domainmodel.activity.domain.impl;

import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContext;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.impl.ActivityContextBase;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public class DomainActivityContextBase
        <THIS extends DomainActivityContextBase<THIS>>

        extends ActivityContextBase<THIS>
        implements DomainActivityContext<THIS> {

    private DataSourceModel dataSourceModel;

    protected DomainActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    @Override
    public THIS setDataSourceModel(DataSourceModel dataSourceModel) {
        this.dataSourceModel = dataSourceModel;
        return (THIS) this;
    }

    @Override
    public DataSourceModel getDataSourceModel() {
        return dataSourceModel;
    }
}
