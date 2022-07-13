package dev.webfx.stack.routing.activity;

/**
 * @author Bruno Salmon
 */
public interface HasActivityContext<C extends ActivityContext<C>> {

    C getActivityContext();

}
