package dev.webfx.stack.framework.client.orm.reactive.dql.statement.conventions;

import javafx.beans.property.Property;
import dev.webfx.stack.framework.shared.orm.dql.DqlStatement;

public interface HasGroupDqlStatementProperty {

    Property<DqlStatement> groupDqlStatementProperty();
    default DqlStatement getGroupDqlStatement() { return groupDqlStatementProperty().getValue();}
    default void setGroupDqlStatement(DqlStatement value) { groupDqlStatementProperty().setValue(value);}

}
