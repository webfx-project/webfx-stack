package dev.webfx.stack.routing.uirouter.activity.presentation.view.impl;

import dev.webfx.stack.routing.uirouter.activity.view.impl.ViewActivityBase;
import javafx.scene.Node;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivity;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContext;
import dev.webfx.stack.routing.uirouter.activity.presentation.view.PresentationViewActivityContextMixin;

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
