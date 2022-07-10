package dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualSelection;

public interface HasGroupVisualSelectionProperty {

    ObjectProperty<VisualSelection> groupVisualSelectionProperty();
    default VisualSelection getGroupVisualSelection() { return groupVisualSelectionProperty().get();}
    default void setGroupVisualSelection(VisualSelection value) { groupVisualSelectionProperty().set(value);}

}
