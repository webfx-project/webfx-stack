package dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualResult;

public interface HasGroupVisualResultProperty {

    ObjectProperty<VisualResult> groupVisualResultProperty();
    default VisualResult getGroupVisualResult() { return groupVisualResultProperty().get();}
    default void setGroupVisualResult(VisualResult value) { groupVisualResultProperty().set(value);}

}
