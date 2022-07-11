package dev.webfx.stack.com.bus;

/**
 * @author Bruno Salmon
 */
public interface BusFactory<O extends BusOptions> {

    Bus createBus(O options);

}
