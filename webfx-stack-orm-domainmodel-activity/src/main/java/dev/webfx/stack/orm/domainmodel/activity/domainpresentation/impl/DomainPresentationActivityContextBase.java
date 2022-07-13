package dev.webfx.stack.orm.domainmodel.activity.domainpresentation.impl;

import dev.webfx.stack.orm.domainmodel.activity.domainpresentation.DomainPresentationActivityContext;
import dev.webfx.stack.orm.domainmodel.activity.domain.DomainActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.impl.PresentationActivityContextBase;
import dev.webfx.stack.routing.uirouter.activity.presentation.logic.PresentationLogicActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public class DomainPresentationActivityContextBase
        <THIS extends DomainPresentationActivityContextBase<THIS, C1, C2, PM>,
                C1 extends PresentationViewActivityContext<C1, PM>,
                C2 extends PresentationLogicActivityContext<C2, PM>,
                PM>

       extends PresentationActivityContextBase<THIS, C1, C2, PM>
        implements DomainPresentationActivityContext<THIS, C1, C2, PM> {

    public DomainPresentationActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    private DataSourceModel dataSourceModel;

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
