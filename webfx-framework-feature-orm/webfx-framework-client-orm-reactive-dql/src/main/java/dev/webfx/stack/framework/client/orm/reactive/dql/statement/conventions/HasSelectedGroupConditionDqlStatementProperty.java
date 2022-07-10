package dev.webfx.stack.framework.client.orm.reactive.dql.statement.conventions;

import javafx.beans.property.Property;
import dev.webfx.stack.framework.shared.orm.dql.DqlStatement;

public interface HasSelectedGroupConditionDqlStatementProperty {

    Property<DqlStatement> selectedGroupConditionDqlStatementProperty();
    default DqlStatement getSelectedGroupConditionDqlStatement() { return selectedGroupConditionDqlStatementProperty().getValue(); }
    default void setSelectedGroupConditionDqlStatement(DqlStatement value) { selectedGroupConditionDqlStatementProperty().setValue(value); }

}
