package dev.webfx.stack.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualResult;

public interface HasMasterVisualResultProperty {

    ObjectProperty<VisualResult> masterVisualResultProperty();

    default VisualResult getMasterVisualResult() { return masterVisualResultProperty().getValue(); }

    default void setMasterVisualResult(VisualResult value) { masterVisualResultProperty().setValue(value); }

}
