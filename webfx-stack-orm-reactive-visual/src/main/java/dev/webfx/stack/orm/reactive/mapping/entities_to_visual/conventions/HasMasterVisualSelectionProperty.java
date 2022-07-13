package dev.webfx.stack.orm.reactive.mapping.entities_to_visual.conventions;

import javafx.beans.property.ObjectProperty;
import dev.webfx.extras.visual.VisualSelection;

public interface HasMasterVisualSelectionProperty {

    ObjectProperty<VisualSelection> masterVisualSelectionProperty();
    
    default VisualSelection getMasterVisualSelection() { return masterVisualSelectionProperty().getValue(); }
    
    default void setMasterVisualSelection(VisualSelection value) { masterVisualSelectionProperty().setValue(value); }
    
}
