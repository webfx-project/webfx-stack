package dev.webfx.stack.orm.expression.terms.function;

import dev.webfx.extras.type.SpecializedTextType;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.builder.ReferenceResolver;
import dev.webfx.stack.orm.expression.builder.ThreadLocalReferenceResolver;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.stack.orm.expression.parser.ExpressionParser;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.extras.type.Type;
import dev.webfx.platform.util.Strings;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class InlineFunction<T> extends Function<T> {

    private final Expression body;

    public InlineFunction(String name, String signature, Type[] argTypes, String body) {
        this(name, Strings.split(signature, ","), argTypes, body, null, null);
    }

    public InlineFunction(String name, String signature, Type[] argTypes, String body, Object domainClass, ParserDomainModelReader modelReader) {
        this(name, Strings.split(signature, ","), argTypes, body, domainClass, modelReader);
    }


    public InlineFunction(String name, String[] argNames, Type[] argTypes, String body) {
        this(name, argNames, argTypes, body ,null, null);
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, String body, Object domainClass, ParserDomainModelReader modelReader) {
        this(name, argNames, argTypes, parseBody(body, argNames, argTypes, domainClass, modelReader));
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, Expression body) {
        super(name, argNames, argTypes, body.getType(), true);
        this.body = body;
    }

    public Expression getBody() {
        return body;
    }

    @Override
    public Object evaluate(Object argument, DomainReader domainReader) {
        try {
            pushArguments(argument);
            return body.evaluate(null, domainReader);
        } finally {
            popArguments();
        }
    }

    @Override
    public int getPrecedenceLevel() {
        return body.getPrecedenceLevel();
    }

    @Override
    public boolean isIdentity() {
        return argNames != null && argNames.length == 1 && argNames[0].equals(body.toString()) && !(getReturnType() instanceof SpecializedTextType);
    }

    private static Expression parseBody(String body, final String[] argNames, final Type[] argTypes, Object domainClass, ParserDomainModelReader modelReader) {
        try {
            ThreadLocalReferenceResolver.pushReferenceResolver(new ReferenceResolver() {
                final Map<String, ArgumentAlias> argumentAliases = new HashMap<>();
                @Override
                public Expression resolveReference(String name) {
                    ArgumentAlias argumentAlias = argumentAliases.get(name);
                    if (argumentAlias != null) // in case it was already resolved, no need to create a new instance, we reuse the existing one
                        return argumentAlias;
                    for (int i = 0; i < argNames.length; i++) {
                        if (name.equals(argNames[i])) {
                            argumentAliases.put(name, argumentAlias = new ArgumentAlias(name, argTypes == null ? null : argTypes[i], i));
                            return argumentAlias;
                        }
                    }
                    return null;
                }
            });
            return ExpressionParser.parseExpression(body, domainClass, modelReader, false);
        } finally {
            ThreadLocalReferenceResolver.popReferenceResolver();
        }

    }

    public void pushArguments(Object argument) { // single argument or array of arguments
        if (argument instanceof ExpressionArray)
            argument = ((ExpressionArray) argument).getExpressions();
        ThreadLocalArgumentStack.pushArgument(argument);
    }

    public void popArguments() {
        ThreadLocalArgumentStack.popArgument();
    }

}
