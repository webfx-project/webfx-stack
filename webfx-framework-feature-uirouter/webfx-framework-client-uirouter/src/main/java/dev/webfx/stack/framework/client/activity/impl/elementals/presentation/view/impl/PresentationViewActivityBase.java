package dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.impl;

import javafx.scene.Node;
import dev.webfx.stack.framework.client.activity.impl.elementals.view.impl.ViewActivityBase;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivity;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.presentation.view.PresentationViewActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public abstract class PresentationViewActivityBase
        <C extends PresentationViewActivityContext<C, PM>, PM>

        extends ViewActivityBase<C>
        implements PresentationViewActivity<C, PM>,
        PresentationViewActivityContextMixin<C, PM> {

    public PresentationViewActivityBase() {
    }

    public void setPresentationModel(PM presentationModel) {
        PresentationViewActivityContextBase.toViewModelActivityContextBase(getActivityContext()).setPresentationModel(presentationModel);
    }

    @Override
    public Node buildUi() {
        return buildPresentationView(getPresentationModel());
    }

    protected Node buildPresentationView(PM pm) {
        createViewNodes(pm);
        return styleUi(assemblyViewNodes(), pm);
    }

    protected abstract void createViewNodes(PM pm);

    protected abstract Node assemblyViewNodes();

    protected Node styleUi(Node uiNode, PM pm) {
        return uiNode;
    }
}
