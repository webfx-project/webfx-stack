package dev.webfx.stack.ui.fxraiser.impl.converters;

import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class ImageToImageViewConverter implements FXValueRaiser {

    @Override
    public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
        if (value instanceof Image && isAssignableFrom(raisedClass, ImageView.class))
            return (T) new ImageView((Image) value);
        return null;
    }

}
