package dev.webfx.stack.framework.client.orm.reactive.dql.statement.conventions;

import javafx.beans.property.Property;
import dev.webfx.stack.framework.shared.orm.dql.DqlStatement;

public interface HasConditionDqlStatementProperty {

    Property<DqlStatement> conditionDqlStatementProperty();
    default DqlStatement getConditionDqlStatement() { return conditionDqlStatementProperty().getValue();}
    default void setConditionDqlStatement(DqlStatement value) { conditionDqlStatementProperty().setValue(value);}

}
