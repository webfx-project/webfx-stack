package dev.webfx.stack.framework.client.activity.impl.composition;

import dev.webfx.stack.framework.client.activity.Activity;
import dev.webfx.stack.framework.client.activity.ActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ComposedActivity
        <C extends ComposedActivityContext<C, C1, C2>,
                C1 extends ActivityContext<C1>,
                C2 extends ActivityContext<C2>>

        extends Activity<C> {
}
