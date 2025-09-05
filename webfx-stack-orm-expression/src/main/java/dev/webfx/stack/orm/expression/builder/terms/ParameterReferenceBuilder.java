package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.ParameterReference;

/**
 * @author Bruno Salmon
 */
public final class ParameterReferenceBuilder extends ExpressionBuilder {

    // For indexed parameters such as $1, $2, etc...
    public int index = -1;

    public String name;
    public ExpressionBuilder rightDot;

    public ParameterReferenceBuilder() {
    }

    public ParameterReferenceBuilder(int index) {
        this.index = index;
    }

    public ParameterReferenceBuilder(String name) {
        this.name = name;
    }

    public ParameterReferenceBuilder(String name, ExpressionBuilder rightDot) {
        this.name = name;
        this.rightDot = rightDot;
    }

    @Override
    public Expression build() {
        if (index >= 1)
            return new ParameterReference<>(index);
        if (name == null)
            return ParameterReference.UNNAMED_PARAMETER_REFERENCE;
        /*
        Expression rd = null;
        if (rightDot != null) {
            rightDot.buildingClass = buildingClass.getDomainModel().getParameterClass(name);
            if (rightDot.buildingClass == null && name.startsWith("this"))
                rightDot.buildingClass = buildingClass.getDomainModel().getClass(name.substring(4));
            if (rightDot.buildingClass != null)
                rd = rightDot.build();
            else // might happen on server side, but we don't need rightDot in that case since we have the value
                name += '.' + rightDot.toString(); // just keep a trace of it in the name
        }
        */
        return new ParameterReference<>(name);
    }
}
