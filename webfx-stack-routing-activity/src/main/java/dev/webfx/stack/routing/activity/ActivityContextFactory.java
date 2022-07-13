package dev.webfx.stack.routing.activity;

/**
 * @author Bruno Salmon
 */
public interface ActivityContextFactory<C extends ActivityContext<C>> {

    C createContext(ActivityContext parentContext);

}
