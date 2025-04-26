package dev.webfx.stack.ui.fxraiser.impl.converters;

import dev.webfx.platform.util.Objects;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import javafx.scene.image.Image;

/**
 * @author Bruno Salmon
 */
public class StringUrlToImageConverter implements FXValueRaiser {

    @Override
    public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
        if (value instanceof String && Objects.isAssignableFrom(raisedClass, Image.class)) {
            String url = (String) value;
            if (url.toLowerCase().matches(".+\\.(png|jpe?g|gif|bmp|webp|svg)$"))
                return (T) new Image(url);
        }
        return null;
    }

}
