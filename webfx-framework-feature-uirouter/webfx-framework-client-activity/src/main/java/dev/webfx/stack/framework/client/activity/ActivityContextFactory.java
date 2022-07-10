package dev.webfx.stack.framework.client.activity;

/**
 * @author Bruno Salmon
 */
public interface ActivityContextFactory<C extends ActivityContext<C>> {

    C createContext(ActivityContext parentContext);

}
