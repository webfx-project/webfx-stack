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

    /**
     * With {@code evaluable} false, this function is compiled INTO SQL wherever it appears, including a
     * select list.
     *
     * <p>The default is true, meaning the caller can work the body out for itself: in a select list the
     * compiler then emits the argument's persistent terms rather than the body, and a client holding the DQL
     * runtime evaluates it. That is right for a presentation function — {@code image(...)}, {@code html(...)}
     * — which has no SQL meaning at all, and 42 domain fields are defined that way.
     *
     * <p>It is wrong for a function whose body only the DATABASE can answer, such as a correlated count. Such
     * a function is also the way a client can use a construct a restricted dialect refuses: the client writes
     * a call, the subquery lives in a body the server wrote, and the client's own statement stays inside the
     * dialect. See {@code docs/security/read-authorization-plan.md}, step 2b.
     */
    public InlineFunction(String name, String signature, Type[] argTypes, String body, Object domainClass, ParserDomainModelReader modelReader, boolean evaluable) {
        this(name, Strings.split(signature, ","), argTypes, body, domainClass, modelReader, evaluable);
    }


    public InlineFunction(String name, String[] argNames, Type[] argTypes, String body) {
        this(name, argNames, argTypes, body ,null, null);
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, String body, Object domainClass, ParserDomainModelReader modelReader) {
        this(name, argNames, argTypes, parseBody(body, argNames, argTypes, domainClass, modelReader));
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, String body, Object domainClass, ParserDomainModelReader modelReader, boolean evaluable) {
        this(name, argNames, argTypes, parseBody(body, argNames, argTypes, domainClass, modelReader), evaluable);
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, Expression body) {
        this(name, argNames, argTypes, body, true);
    }

    public InlineFunction(String name, String[] argNames, Type[] argTypes, Expression body, boolean evaluable) {
        super(name, argNames, argTypes, body.getType(), evaluable);
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
