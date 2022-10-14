package dev.webfx.stack.ui.fxraiser.impl;

/**
 * @author Bruno Salmon
 */
public interface ValueFormatter {

    <T> Object formatValue(Object value, Class<T> raisedClass, Object... args);

}
