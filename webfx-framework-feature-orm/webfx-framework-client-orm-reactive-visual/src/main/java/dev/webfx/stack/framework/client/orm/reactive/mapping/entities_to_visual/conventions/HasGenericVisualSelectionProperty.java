package dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualSelection;

public interface HasGenericVisualSelectionProperty {

    ObjectProperty<VisualSelection> genericVisualSelectionProperty();
    
    default VisualSelection getGenericVisualSelection() { return genericVisualSelectionProperty().getValue(); }
    
    default void setGenericVisualSelection(VisualSelection value) { genericVisualSelectionProperty().setValue(value); }
    
}
