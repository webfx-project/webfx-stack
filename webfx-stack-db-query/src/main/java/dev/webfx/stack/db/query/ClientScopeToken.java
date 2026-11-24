package dev.webfx.stack.db.query;

/**
 * Which data scope a client's query compiles under — asked at the endpoint, where the caller still is.
 *
 * <p><b>Asked HERE and nowhere else, which is the boundary that matters.</b> The three generic query
 * endpoints are the only place a client's own statement arrives; an endpoint's internal lookup reaches the
 * query service directly and never passes this way. Deciding the scope by the thread's origin instead would
 * have scoped those lookups too — and a lookup that authorizes something by reading through the very filter
 * it is checking is not a check. {@code ClientQueryGuard} draws the same boundary for the same reason, and
 * this is deliberately the same door.
 *
 * <p><b>The token names one of a FIXED set of scopes — never the caller.</b> Compiled sql is cached per
 * (statement, token), so a token that varied per caller would grow that cache with the callers;
 * {@code ScopedCompilationCache} refuses it by name once it sees too many. Who is asking travels separately,
 * as a bound caller parameter — see {@link ServerCallerParameters}.
 *
 * <p>Null when nothing is registered, which is the state until a read-scope provider exists: every query then
 * compiles exactly as it did before scopes, and {@link QueryArgument#getScopeToken()} stays null so arguments
 * still compare equal and subscriptions still share. Once a scope is REQUIRED, the absent token stops meaning
 * "unscoped" and starts meaning refuse — see {@link #forCaller()}.
 *
 * @author Claude Code
 */
public final class ClientScopeToken {

    /**
     * Answers which scope this caller reads under. Implemented where a principal means something — the
     * framework has no notion of a back office or a member — and asked only on the endpoint thread, before
     * the first async hop, for the reason {@link ServerCallerParameters} gives.
     */
    public interface Resolver {
        /** The scope for the caller on this thread, or null to compile unscoped. */
        Object tokenForCaller();

        /**
         * Whether a scope MUST be resolved before a client's read runs, so that no token refuses the read
         * instead of compiling it unscoped.
         *
         * <p><b>Answer it from the same state that drives enforcement</b> — the set of enforced roots, not a
         * flag beside it. A second switch is the one that gets forgotten on the day a root is enforced, and
         * the cost of forgetting it is every centre's rows.
         *
         * <p>Called on every client read, before {@link #tokenForCaller()}, so it must be a cheap constant
         * read: it must not consult the caller, touch a cache, or depend on anything that can fail. A
         * resolver that throws here is taken to mean "not required" — {@link #forCaller()} says why that
         * residual is deliberate, and why it is not the one that matters.
         */
        default boolean isScopeRequired() {
            return false;
        }
    }

    /**
     * What a client is told when its read had to be scoped and the scope could not be resolved.
     *
     * <p><b>Retriable, and it says so.</b> The scopes a caller reads under are assembled asynchronously and
     * peeked synchronously here, so the common miss is a scope that is merely not computed yet — the same
     * shape as {@link ServerCallerParameters#CALLER_VALUE_UNAVAILABLE_REFUSED} next door, and a retry fixes
     * it for the same reason. Short and schema-free like the guard's refusals.
     *
     * <p>It is a new message, so it needs a CloudWatch metric filter of its own, and that filter must NOT
     * page: it counts. It should read zero while nothing is enforced.
     */
    public static final String SCOPE_UNAVAILABLE_REFUSED =
        "The caller's data scope could not be determined, so this query was not run — retry";

    /**
     * Thrown by {@link #forCaller()} when a scope is required and none could be resolved.
     *
     * <p>The three client query endpoints catch it and fail the call with
     * {@link #SCOPE_UNAVAILABLE_REFUSED}, which is the shape every other refusal there already has — and the
     * push endpoint catches it BEFORE it opens a subscription, so nothing is created that would re-fire
     * unscoped for the rest of its life.
     */
    public static final class ScopeUnavailableException extends RuntimeException {
        public ScopeUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * Volatile because the invariant above is "under enforcement, no client read compiles unscoped", and a
     * thread that cannot yet see the boot-time registration reads null here and returns before
     * {@link Resolver#isScopeRequired()} is ever consulted — unscoped, silently, exactly the answer this
     * class now exists to refuse. {@code ClientQueryGuard.inspector} is volatile for the same reason.
     */
    private static volatile Resolver resolver;

    public static void registerResolver(Resolver resolver) {
        ClientScopeToken.resolver = resolver;
    }

    /**
     * The scope for the caller on this thread, or null to compile unscoped.
     *
     * <p><b>Open while nothing is enforced, closed the moment something is</b> — the shape
     * {@code ClientQueryGuard.decide} already uses against an empty deny list, for the same reason. The two
     * states are genuinely different, and reading them as one is how this was wrong:
     *
     * <ul>
     *   <li><b>Nothing enforced.</b> No scope condition is injected anywhere, so an unscoped compile is
     *       precisely what every query did before scopes existed. A resolver that throws must not take the
     *       read down with it: it would refuse reads to protect nothing.</li>
     *   <li><b>A root enforced.</b> Its condition is the only thing between the caller and every centre's
     *       rows, and {@code ClientReadScope.conditionFor} injects nothing for a null token. So a resolver
     *       that throws, or that cannot say which scope this caller reads under, would hand back the whole
     *       table — silently, with a successful-looking result. Refused instead, retriably.</li>
     * </ul>
     *
     * <p><b>The residual, stated rather than hidden:</b> {@link Resolver#isScopeRequired()} is itself read
     * in a try, and a throw from it is read as "not required". That keeps a broken predicate from refusing
     * every client read on the server at once — the failure mode this arc has already had twice — and it is
     * safe to prefer because the two methods differ in kind: {@code tokenForCaller} parses a principal and
     * peeks a cache and can genuinely fail, while {@code isScopeRequired} is contracted to be a constant
     * read. The dangerous one fails closed.
     *
     * @throws ScopeUnavailableException when a scope is required and none could be resolved
     */
    public static Object forCaller() {
        Resolver r = resolver;
        if (r == null)
            return null;
        boolean required = isScopeRequired(r);
        Object token;
        try {
            token = r.tokenForCaller();
        } catch (Throwable e) {
            if (required)
                throw new ScopeUnavailableException(SCOPE_UNAVAILABLE_REFUSED);
            return null;
        }
        // The null that used to mean "unscoped" unconditionally. Under enforcement it is the one answer a
        // read must not be run on, and it arrives both from a resolver that could not place the caller and
        // from one that found no scope to apply — neither of which is a licence to read everything.
        if (token == null && required)
            throw new ScopeUnavailableException(SCOPE_UNAVAILABLE_REFUSED);
        return token;
    }

    /** See the residual paragraph on {@link #forCaller()}: unanswerable is read as not required. */
    private static boolean isScopeRequired(Resolver r) {
        try {
            return r.isScopeRequired();
        } catch (Throwable e) {
            return false;
        }
    }

    private ClientScopeToken() {}
}
