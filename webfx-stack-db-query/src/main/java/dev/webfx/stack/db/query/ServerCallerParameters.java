package dev.webfx.stack.db.query;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;

/**
 * Resolves the terms only the server can value — {@code CALLER_PERSON} and its kin — and attaches their
 * values to a client's query before it runs.
 *
 * <p><b>Called from the ENDPOINT, synchronously.</b> The principal lives on the thread only until the first
 * async hop, and the query interceptor can go async before it binds anything (it waits for the local data
 * source on a cold server), so resolving there would read null and fail open. The endpoint is where the
 * caller is still known, and it is also where a client's query is already being checked.
 *
 * <p><b>The values are appended to the PARAMETERS, not carried beside them.</b> A push subscription is keyed
 * by its {@link QueryArgument}, whose equality compares parameter values and would ignore a field held to one
 * side — so two callers running the same statement would compare equal, share one subscription, and one would
 * be served rows fetched for the other. Putting the value where equality can see it makes them separate
 * streams by construction rather than by remembering to.
 *
 * <p>They go at the END, named, which is the shape {@code reorderNamedParameters} binds: the caller's own
 * positional block first, then names above it.
 *
 * @author Claude Code
 */
public final class ServerCallerParameters {

    /**
     * Answers who is asking. Implemented where the principal means something — the framework has no notion of
     * a person or an account — and asked only on the endpoint thread.
     */
    public interface Resolver {
        /**
         * The value for one reserved name, null when there is no caller, or {@link #UNAVAILABLE} when there
         * is one but the value is not known yet.
         *
         * <p>Null is a legitimate answer: an anonymous booker has no person, and a predicate comparing
         * against null matches nothing, which is the safe direction. {@link #UNAVAILABLE} is the answer that
         * is NOT legitimate to bind — see it for why the two had to stop being the same null.
         */
        Object resolveCallerValue(String reservedName);
    }

    /**
     * What a resolver answers for a name it cannot value YET, as opposed to one whose value is null.
     *
     * <p><b>They were the same null, and they are opposite answers.</b> Null means there is no such caller,
     * and a predicate comparing a column against null matches no rows — right for an anonymous booker, and
     * the safe direction. "Not computed yet" bound the same null and produced the same empty result for a
     * caller who may in fact read a great deal: a screen that is silently empty, with nothing in any log to
     * say why, and no way afterwards to tell it from a caller who really may read nothing. Telling the two
     * apart is what lets the second be refused while the first still runs.
     *
     * <p><b>It never travels on a QueryArgument.</b> {@link #attach} resolves it one way or the other — a
     * refusal when the statement is known to need the value, null when the names were a guess — so no push
     * subscription can be opened carrying it and no re-fire can inherit it. A subscription that stored it
     * would fail every re-fire for as long as it lived, which is a worse failure than the one this fixes.
     * The binding seam refuses one anyway, as a floor under that.
     */
    public static final Object UNAVAILABLE = new Object() {
        @Override
        public String toString() {
            return "<unavailable>";
        }
    };

    /**
     * What a client is told when its read needed a value the server could not supply yet.
     *
     * <p><b>Retriable, and it says so</b>, because it is the one refusal here that a retry fixes: the value
     * is missing only until the computation that fills it finishes, and the miss itself starts that
     * computation. Short and schema-free like the guard's refusals next door, for the same reason.
     *
     * <p>It is a new message, so it needs a CloudWatch metric filter of its own, and that filter must NOT
     * page: this refusal is expected at a rate of about one caller per server process.
     */
    public static final String CALLER_VALUE_UNAVAILABLE_REFUSED =
        "The caller's permissions are not ready yet, so this query was not run — retry";

    /**
     * Thrown by {@link #attach} when a value the statement is KNOWN to need is unavailable.
     *
     * <p>A throw rather than a returned refusal string because {@link #attach} answers with an argument, and
     * the alternative — asking a second time, before attaching — would peek the caller's scope twice per
     * statement and count every miss twice. The three client query endpoints catch it and fail the call with
     * {@link #CALLER_VALUE_UNAVAILABLE_REFUSED}, which is the shape every other refusal there already has.
     */
    public static final class CallerValueUnavailableException extends RuntimeException {
        public CallerValueUnavailableException(String message) {
            super(message);
        }
    }

    private static Resolver resolver;

    public static void registerResolver(Resolver resolver) {
        ServerCallerParameters.resolver = resolver;
    }

    /**
     * The names this attaches. Kept in step with the terms registered in {@code Function}; a term whose name
     * is missing here compiles to a parameter nothing supplies, and the binding seam then refuses the query
     * by name rather than letting it reach the driver.
     */
    private static final String[] RESERVED_NAMES = {"caller.person", "caller.account",
        "caller.grantedEvents", "caller.grantedOrganizations"};

    /**
     * The prefix every server-supplied name carries — the same as {@code ServerParameterFunction.PARAMETER_NAME_PREFIX},
     * which this module cannot see. Matched as a prefix rather than against {@link #RESERVED_NAMES}, so a term
     * registered later is covered before anyone remembers to list it.
     */
    private static final String RESERVED_PREFIX = "caller.";

    /** Whether any of these names is one only the server may supply (a client sending one is refused). */
    public static boolean suppliesServerName(String[] parameterNames) {
        if (parameterNames != null)
            for (String name : parameterNames)
                if (name != null && name.startsWith(RESERVED_PREFIX))
                    return true;
        return false;
    }

    /**
     * True when the statement could possibly use one of these terms.
     *
     * <p>A TEXT test, deliberately, and it is the reason this is affordable at all. Attaching unconditionally
     * would make every argument caller-specific, and since a push subscription is keyed by the argument, every
     * viewer of a shared query would get a stream of their own — deduplication across clients is the thing
     * that makes push affordable, and it would collapse.
     *
     * <p>Sound for a term the CLIENT wrote: one has to appear literally in the statement to be parsed as one.
     * It CAN fire on a statement that merely mentions the text inside a literal, and that costs a value nobody
     * binds, which the binding seam drops.
     *
     * <p><b>It is NOT sound for a term the SERVER injects.</b> A data scope condition is added by the compiler
     * and appears nowhere in this text, so a statement reading a scoped entity needs {@code CALLER_PERSON}
     * while saying nothing of the kind — the compiler emits the parameter slot and nothing fills it. That is
     * what {@link #attach(QueryArgument, Object)} asks the inspector about; this text test remains the answer
     * for an unscoped caller, where it is exactly right and costs nothing.
     */
    private static boolean mayUseCallerTerm(String statement) {
        return statement != null && statement.contains("CALLER_");
    }

    /**
     * The argument to run: the original when no caller term can be present, otherwise a copy with the
     * resolved values appended and named.
     */
    public static QueryArgument attach(QueryArgument argument) {
        return attach(argument, null);
    }

    /**
     * The argument to run, with the names this statement will need resolved and appended.
     *
     * <p><b>Which names, and why it cannot simply be all of them.</b> Appending unconditionally would make
     * every argument caller-specific, and a push subscription is keyed by its argument — so every viewer of a
     * shared query would get a stream of their own and the deduplication that makes push affordable would
     * collapse. That is the constraint {@link #mayUseCallerTerm} was built around, and it still holds.
     *
     * <p><b>With a scope token the text test is no longer sound</b>, because the condition the compiler
     * injects is not in the client's text. So the inspector is asked instead: it compiles the statement under
     * the token — a cached compile the interceptor is about to repeat — and reports the names the compiled
     * form actually binds. That is precise in both directions: a statement reading a scoped entity gets the
     * value even though it never mentions it, and a statement reading nothing scoped keeps an argument equal
     * to every other caller's, so those streams still share.
     *
     * <p>When the inspector cannot answer, every reserved name is attached rather than none. That costs
     * sharing for one statement; the other direction costs a parameter slot nothing fills, which fails the
     * query.
     *
     * @throws CallerValueUnavailableException when a value this statement is KNOWN to need is not available
     */
    public static QueryArgument attach(QueryArgument argument, Object scopeToken) {
        Resolver r = resolver;
        if (r == null || argument == null)
            return argument;
        String[] attachNames;
        // Whether these names are the ones the statement WILL bind, or a guess that merely covers them. It
        // decides what an UNAVAILABLE value costs: a refusal where the statement is known to need it,
        // nothing where it may well not use it at all.
        boolean exactlyWhatIsNeeded;
        if (scopeToken == null) {
            // No scope, so nothing can be injected and the client's own text is the whole truth. Byte for
            // byte what this did before scopes existed.
            if (!mayUseCallerTerm(argument.getStatement()))
                return argument;
            attachNames = RESERVED_NAMES;
            exactlyWhatIsNeeded = false; // a text match on the client's statement, deliberately loose
        } else {
            attachNames = ClientQueryGuard.serverCallerNamesNeeded(argument, scopeToken);
            exactlyWhatIsNeeded = attachNames != null; // it compiled the statement, so it knows
            if (!exactlyWhatIsNeeded) // could not say - attach everything rather than bind nothing
                attachNames = RESERVED_NAMES;
            else if (attachNames.length == 0) // said, and the answer is none: leave the argument shareable
                return argument;
        }
        Object[] parameters = argument.getParameters();
        String[] names = argument.getParameterNames();
        int suppliedCount = Arrays.length(parameters);
        Object[] newParameters = new Object[suppliedCount + attachNames.length];
        String[] newNames = new String[Arrays.length(names) + attachNames.length];
        for (int i = 0; i < suppliedCount; i++)
            newParameters[i] = parameters[i];
        for (int i = 0; i < Arrays.length(names); i++)
            newNames[i] = names[i];
        for (int i = 0; i < attachNames.length; i++) {
            newParameters[suppliedCount + i] = resolvedValue(r, attachNames[i], exactlyWhatIsNeeded);
            newNames[Arrays.length(names) + i] = attachNames[i];
        }
        return QueryArgument.builder().copy(argument).setParameters(newParameters).setParameterNames(newNames).build();
    }

    /**
     * One resolved value, and the decision about an unavailable one.
     *
     * <p>Refused when the names came from the inspector, which compiled the statement and therefore knows
     * this one will be bound: binding null there is the silently empty screen, and the caller is better told
     * to retry — by then the lookup that was missing has been started by the miss itself.
     *
     * <p>Bound as null when the names were a guess, which is what happened before any of this existed.
     */
    private static Object resolvedValue(Resolver r, String name, boolean exactlyWhatIsNeeded) {
        Object value = r.resolveCallerValue(name);
        if (value != UNAVAILABLE)
            return value;
        if (exactlyWhatIsNeeded) {
            reportRefusal(name);
            throw new CallerValueUnavailableException(CALLER_VALUE_UNAVAILABLE_REFUSED);
        }
        reportGuessedUnavailable(name);
        return null;
    }

    /**
     * How often each case has happened. Plain longs rather than atomics: the reports are throttled to powers
     * of ten, so a count that races by one still answers the only question asked of it — does this happen,
     * and roughly how often. The module is compiled into the browser too, where an atomic would buy nothing.
     */
    private static long refusedCount, guessedUnavailableCount;

    /**
     * The refusal, in the server's own log as well as in the client's reply.
     *
     * <p>Without this the refusal would be visible only to the caller it refused, and an operator asking
     * "did anyone hit this?" would have nothing to read. Powers of ten rather than every line because the
     * misses come in bursts — one caller's page load fires every query it needs at once, and 28 of them in
     * 91 milliseconds is what production measured on 13 November.
     *
     * <p>It is a new message, so it needs a CloudWatch metric filter of its own, and that filter must NOT
     * page: this is an expected event at about one caller per server process.
     */
    private static synchronized void reportRefusal(String name) {
        long n = ++refusedCount;
        if (isPowerOfTen(n))
            Console.log("🛡 Refused a client query: it binds a caller value that is not available yet ("
                + name + ", ×" + n + ") — the caller may retry, and by then it will be");
    }

    /**
     * The one gap the refusal leaves, counted rather than argued about.
     *
     * <p>The inspector answers precisely except when it cannot compile the statement — in practice on a
     * server whose local data source has not come up yet, which is also the moment when nothing has been
     * computed for anybody. A read in that window that genuinely needs the value still binds null and still
     * comes back empty. Refusing instead would refuse every read in that window, the ones needing nothing
     * included, which is a far wider blast radius for a case nothing has yet observed — so it is measured
     * first. If this line never appears, the gap is theoretical.
     */
    private static synchronized void reportGuessedUnavailable(String name) {
        long n = ++guessedUnavailableCount;
        if (isPowerOfTen(n))
            Console.log("⚠️ A caller value was unavailable where the attached names were a guess, so it was"
                + " bound as null (" + name + ", ×" + n + "). A read needing it comes back EMPTY.");
    }

    /** True for 1, 10, 100, … — the thresholds at which a count is worth another line. */
    private static boolean isPowerOfTen(long count) {
        if (count < 1)
            return false;
        while (count % 10 == 0)
            count /= 10;
        return count == 1;
    }
}
