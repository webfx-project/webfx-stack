package dev.webfx.stack.orm.expression.terms.function;

import dev.webfx.extras.type.Type;

/**
 * A zero-argument term whose value only the SERVER can supply — the caller's identity, above all.
 *
 * <p>Written like {@code CURRENT_DATE} and parsed the same way, but it cannot compile to a SQL keyword the
 * way that one does: the database knows today's date and does not know who is asking, since reads run on a
 * pooled connection under one role.
 *
 * <p><b>It compiles to a bound parameter, and it must never compile to a literal.</b> Compiled SQL is cached
 * by the statement's TEXT, and two callers send identical text — so a literal would bake the first caller's
 * identity into the cached SQL and serve it to the second. That is not a degraded check, it is one member's
 * data handed to another. The parameter is numbered above the caller's own {@code $n}, and its value is
 * appended at execution.
 *
 * <p>It is a TERM and not a parameter for a reason worth keeping: a parameter arrives through the channel the
 * client fills, so its safety would rest on the server always overwriting whatever the client sent. A term
 * has no slot for a client to fill, so forging it is not something that can be attempted.
 *
 * @author Claude Code
 */
public class ServerParameterFunction<T> extends Function<T> {

    /** Reserved prefix for these names, so an executor can tell a server-supplied value from a caller's. */
    public static final String PARAMETER_NAME_PREFIX = "caller.";

    private final String parameterName;
    private final boolean arrayValued;

    public ServerParameterFunction(String name, String parameterName, Type returnType) {
        this(name, parameterName, returnType, false);
    }

    public ServerParameterFunction(String name, String parameterName, Type returnType, boolean arrayValued) {
        // evaluable=false: no client can work this out for itself, so it is compiled into SQL wherever it
        // appears, including a select list. keyword=true: written bare, with no parentheses.
        super(name, returnType, false, true);
        this.parameterName = parameterName;
        this.arrayValued = arrayValued;
    }

    /** The reserved name under which the server supplies this term's value at execution. */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Whether the server supplies MANY values rather than one, so that {@code x in THIS_TERM} means
     * membership of an array.
     *
     * <p>Declared on the term rather than inferred, because the two readings are both valid SQL and only
     * one is ever meant: {@code x in CALLER_PERSON} is a scalar comparison, {@code x in
     * CALLER_GRANTED_EVENTS} is {@code x = any(…)}. Postgres cannot bind an array to a scalar
     * {@code in ($n)}, so getting this wrong is a runtime bind failure rather than a wrong answer -
     * which is the better of the two, but only just.
     */
    public boolean isArrayValued() {
        return arrayValued;
    }
}
