package dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualResult;

public interface HasSlaveVisualResultProperty {

    ObjectProperty<VisualResult> slaveVisualResultProperty();

    default VisualResult getSlaveVisualResult() { return slaveVisualResultProperty().getValue(); }

    default void setSlaveVisualResult(VisualResult value) { slaveVisualResultProperty().setValue(value); }

}
