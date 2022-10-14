package dev.webfx.stack.ui.fxraiser;

public interface FXValueRaiser {

    <T> T raiseValue(Object value, Class<T> raisedClass, Object... args);

}
