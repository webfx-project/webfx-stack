package dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl;

import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.impl.ViewActivityContextBase;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.ViewDomainActivityContext;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public class ViewDomainActivityContextBase
        <THIS extends ViewDomainActivityContextBase<THIS>>

        extends ViewActivityContextBase<THIS>
        implements ViewDomainActivityContext<THIS> {

    private DataSourceModel dataSourceModel;

    protected ViewDomainActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    @Override
    public THIS setDataSourceModel(DataSourceModel dataSourceModel) {
        this.dataSourceModel = dataSourceModel;
        return (THIS) this;
    }

    @Override
    public DataSourceModel getDataSourceModel() {
        if (dataSourceModel != null)
            return dataSourceModel;
        ActivityContext parentContext = getParentContext();
        if (parentContext instanceof DomainActivityContext)
            return ((DomainActivityContext) parentContext).getDataSourceModel();
        return null;
    }
}
