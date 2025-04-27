package dev.webfx.stack.orm.reactive.entities.entities_to_grid;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.domainmodel.formatter.GenericFormatterFactory;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.function.Call;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public class EntityColumnImpl<E extends Entity> implements EntityColumn<E> {

    private final String expressionDefinition;
    protected Expression<E> expression;
    protected Expression<E> displayExpression;
    protected ValueFormatter displayFormatter;
    protected Object label;
    protected final ReadOnlyAstObject json;
    private Boolean isForeignObject;
    private DomainClass domainClass;
    private DomainClass foreignClass;
    private String foreignAlias;
    private Expression<?> foreignFields;
    private String foreignCondition;
    private String foreignOrderBy;
    private String foreignSearchCondition;
    private Expression<E> applicableCondition;

    protected EntityColumnImpl(String expressionDefinition, Expression<E> expression, Object label, ValueFormatter displayFormatter, ReadOnlyAstObject json) {
        this.expressionDefinition = expressionDefinition;
        this.displayFormatter = displayFormatter;
        this.label = label;
        this.json = json;
        setExpression(expression);
    }

    protected Expression<?> getTopRightExpression() {
        return getTopRightExpression(expression);
    }

    protected static Expression<?> getTopRightExpression(Expression<?> expression) {
        while (true) {
            if (expression instanceof Call) {
                Call<?> call = (Call<?>) expression;
                if (call.getFunction().isIdentity())
                    expression = call.getOperand();
            }
            Expression<?> forwardingTypeExpression = expression.getForwardingTypeExpression();
            if (forwardingTypeExpression == expression)
                break;
            if (expression instanceof As) { // to have 'acco' label instead of 'listing_acco'
                expression = forwardingTypeExpression;
                break;
            }
            if (expression instanceof Dot)
                expression = forwardingTypeExpression;
            else
                break;
        }
        return expression;
    }

    private void setExpression(Expression<E> expression) {
        this.expression = expression;
        // If no display formatter is passed, trying to find a generic one based on the expression type
        if (displayFormatter == null && expression != null) {
            displayFormatter = GenericFormatterFactory.createGenericFormatter(expression.getType());
        }
    }

    @Override
    public Expression<E> getExpression() {
        return expression;
    }

    @Override
    public DomainClass getForeignClass() {
        if (isForeignObject == null) {
            Expression<?> topRightExpression = expression.getFinalForwardingTypeExpression();
            if (topRightExpression instanceof DomainField)
                foreignClass = ((DomainField) topRightExpression).getForeignClass();
            isForeignObject = foreignClass != null;
        }
        return foreignClass;
    }

    @Override
    public String getForeignAlias() {
        return foreignAlias = getForeignJsonOrDomainAttribute(foreignAlias, "foreignAlias", null, DomainField::getForeignAlias, false);
    }

    @Override
    public Expression<?> getForeignColumns() {
        return foreignFields = getForeignJsonOrDomainAttribute(foreignFields, "foreignColumns", DomainClass::getForeignFields, null, true);
    }

    @Override
    public String getForeignWhere() {
        return foreignCondition = getForeignJsonOrDomainAttribute(foreignCondition, "foreignWhere", null, DomainField::getForeignCondition, false);
    }

    @Override
    public String getForeignOrderBy() {
        return foreignOrderBy = getForeignJsonOrDomainAttribute(foreignOrderBy, "foreignOrderBy", null, DomainField::getForeignOrderBy, false);
    }

    @Override
    public String getForeignSearchCondition() {
        return foreignSearchCondition = getForeignJsonOrDomainAttribute(foreignSearchCondition, "foreignSearchCondition", DomainClass::getSearchCondition, null, false);
    }

    @Override
    public Expression<E> getApplicableCondition() {
        return applicableCondition = getForeignJsonOrDomainAttribute(applicableCondition, "applicableCondition", null, DomainField::getApplicableCondition, true);
    }

    private <A> A getForeignJsonOrDomainAttribute(A currentAttribute, String jsonKey, Function<DomainClass, ?> classAttributeGetter, Function<DomainField, ?> fieldAttributeGetter, boolean expression) {
        DomainClass domainClass = jsonKey.startsWith("foreign") ? getForeignClass() : this.domainClass;
        if (currentAttribute == null && domainClass != null) {
            Object attributeSource = json == null ? null : json.getString(jsonKey);
            if (attributeSource == null && classAttributeGetter != null)
                attributeSource = classAttributeGetter.apply(domainClass);
            if (attributeSource == null && fieldAttributeGetter != null) {
                Expression<?> topRightExpression = getTopRightExpression();
                if (topRightExpression instanceof DomainField)
                    attributeSource = fieldAttributeGetter.apply((DomainField) topRightExpression);
            }
            if (attributeSource != null)
                currentAttribute = expression && attributeSource instanceof String ? (A) domainClass.parseExpression((String) attributeSource) : (A) attributeSource;
        }
        return currentAttribute;
    }

    @Override
    public Expression<E> getDisplayExpression() {
        if (displayExpression == null)
            displayExpression = getForeignColumns() == null ? expression : new Dot<>(expression, foreignFields);
        return displayExpression;
    }

    @Override
    public ValueFormatter getDisplayFormatter() {
        return displayFormatter;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return json != null && Boolean.TRUE.equals(json.getBoolean("readOnly")) || expression == null || !expression.isEditable();
    }

    @Override
    public EntityColumn<E> parseExpressionDefinitionIfNecessary(DomainModel domainModel, Object domainClassId) {
        if (domainClass == null)
            domainClass = domainModel.getClass(domainClassId);
        if (expression == null)
            setExpression(domainModel.parseExpression(expressionDefinition, domainClassId));
        return this;
    }
}
