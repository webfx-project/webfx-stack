package dev.webfx.stack.db.submit;

import dev.webfx.platform.async.Future;

import java.util.Map;

/**
 * Which entities may not be written without the application's say-so, and who to ask.
 *
 * <p>Registry inversion, like {@code RestrictedPrincipalRegistry} and {@code AuditActorRegistry}: the
 * framework owns one enforcement point that a caller cannot route around, and the application owns the
 * policy. The alternative — teaching the DQL layer what an authorization is — would put a dependency on
 * the authz stack into the path every write takes, to express a rule only the application knows.
 *
 * <p><b>The framework enforces the answer it is given and holds no policy of its own</b>, deliberately.
 * There is no "report only" switch here, because a leniency knob in the framework is a knob nobody can
 * see from the application that owns the rule: whether to deny or merely observe is part of the policy,
 * so it belongs with the authorizer, which can also explain itself in the log. What this class
 * guarantees is narrower and more useful — that the question is always asked.
 *
 * <p>With nothing registered, nothing is protected and every write proceeds exactly as before. That is
 * what lets this ship ahead of any policy.
 *
 * @author Bruno Salmon
 */
public final class ProtectedEntityWriteRegistry {

    private ProtectedEntityWriteRegistry() {}

    /** The three ways a DQL statement can change rows. */
    public enum WriteVerb { INSERT, UPDATE, DELETE }

    /**
     * Everything the seam could determine about one write.
     *
     * @param writtenFields the fields this statement SETS, empty for a delete
     * @param writtenValues those fields' values where they resolve to a scalar — an insert names its
     *                      owner here ({@code insert Document set person=$1}), which is how a new row's
     *                      ownership can be judged without a row existing to look up
     * @param targetId      the id of the row being changed, when the statement selects one by primary
     *                      key — and <b>null when it could not be determined</b>, which covers a delete
     *                      or update whose WHERE is anything more interesting than {@code id = value}.
     *                      <p><b>Null is not "no constraint", it is "unknown".</b> A policy that decides
     *                      by ownership must DENY on null: a statement whose target this could not read
     *                      is precisely the statement that would be used to reach somebody else's row.
     *                      Reading null as "not applicable" would make the check optional at the
     *                      attacker's discretion.
     */
    public record WriteRequest(
        String entityName,
        WriteVerb verb,
        String[] writtenFields,
        Map<String, Object> writtenValues,
        Object targetId
    ) {}

    @FunctionalInterface
    public interface WriteAuthorizer {
        /**
         * @return a future true if this write may proceed. A future false, or a failure, denies it — the
         *         caller treats anything that is not an explicit yes as no, so an authorizer that throws
         *         or times out withholds the write rather than waving it through.
         */
        Future<Boolean> isWriteAuthorized(WriteRequest request);
    }

    /** Told after a protected write has actually happened — see {@link #registerWriteObserver}. */
    @FunctionalInterface
    public interface WriteObserver {
        void onProtectedWriteSucceeded(String entityName, WriteVerb verb);
    }

    /**
     * Told when a submit arrives that is NOT DQL, and so will reach the database as written.
     *
     * <p>Reported from the framework because only the DQL layer can still tell: it inspects the
     * statement's language before translating, and afterwards a translated DQL statement and a raw one
     * are the same thing — a SQL string with no language. Judged by the application because whether a
     * raw statement matters depends on WHO sent it, which is a question about origin and identity that
     * this layer has no business knowing.
     */
    @FunctionalInterface
    public interface RawStatementObserver {
        void onNonDqlSubmit(String language, String statement);
    }

    private static volatile RawStatementObserver rawStatementObserver;

    public static void registerRawStatementObserver(RawStatementObserver observer) {
        rawStatementObserver = observer;
    }

    /** Never throws into the caller: observing a statement must not be able to fail one. */
    public static void notifyNonDqlSubmit(String language, String statement) {
        RawStatementObserver observer = rawStatementObserver;
        if (observer != null) {
            try {
                observer.onNonDqlSubmit(language, statement);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /**
     * Told about every write the application says it wants to SEE, whether or not anything protects it.
     *
     * <p>Separate from {@link WriteAuthorizer} because it answers a different question and must not be
     * able to answer the authorizer's. An authorizer decides; an inspector watches. Conflating them —
     * by registering every entity as protected and returning true from the authorizer — would have
     * inherited the authorizer's fail-closed rules, so an unparseable statement would start being
     * REFUSED the moment somebody turned observation on. Observation that can refuse is not
     * observation.
     *
     * <p><b>The application decides what is worth looking at, not this layer.</b> {@link #isInspecting}
     * is asked first, synchronously, before anything is parsed — which is what keeps "only client
     * traffic" expressible without this module learning what a client is. The same division as
     * {@link RawStatementObserver}: the framework can see the statement, only the application can see
     * who sent it.
     *
     * <p>Cost, stated plainly because it is the reason this is opt-in: while an inspector is inspecting,
     * every write it is interested in is PARSED, where the protected path parses only the few statements
     * that mention a protected name. With no inspector registered, or one that says no, a write pays one
     * volatile read.
     */
    public interface WriteInspector {

        /**
         * Asked before the statement is parsed: is this caller's write one you want reported?
         *
         * <p>Called on the caller's own thread, before any async hop, so an implementation may read
         * request-scoped state (the origin, the principal) that would be gone a moment later.
         */
        boolean isInspecting();

        /**
         * One write, parsed into the same shape an authorizer would judge. Cannot refuse it.
         *
         * <p><b>The request carries {@link WriteRequest#writtenValues}, which is members' data</b> —
         * names, addresses, emails, whatever the statement set. It is passed because an authorizer
         * needs it to judge ownership, and an inspector is handed the same record rather than a
         * lesser one. An implementation that logs, exports or stores what it is told here has put
         * personal data somewhere the database's erasure path does not reach. Report the field NAMES.
         */
        void onWrite(WriteRequest request);

        /**
         * A statement the inspector was interested in and that would not parse.
         *
         * <p>Worth reporting rather than dropping: on the protected path an unparseable statement is
         * refused, so it never becomes an observation, and an inventory that silently omitted them
         * would describe a client's traffic as tidier than it is.
         */
        default void onUnparseableStatement(String statement) {}
    }

    private static volatile WriteInspector inspector;

    public static void registerWriteInspector(WriteInspector writeInspector) {
        inspector = writeInspector;
    }

    /**
     * Whether this particular write should be parsed and reported — false whenever asking throws, so a
     * broken inspector observes nothing rather than failing the writes it was watching.
     */
    public static boolean isInspectingWrite() {
        WriteInspector currentInspector = inspector;
        if (currentInspector == null)
            return false;
        try {
            return currentInspector.isInspecting();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Never throws into the caller: watching a write must not be able to fail one. */
    public static void notifyWriteInspected(WriteRequest request) {
        WriteInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onWrite(request);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Never throws into the caller, for the same reason as {@link #notifyWriteInspected}. */
    public static void notifyUnparseableStatement(String statement) {
        WriteInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onUnparseableStatement(statement);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static volatile WriteAuthorizer authorizer;
    private static volatile WriteObserver observer;
    /** Lower-cased, so the pre-filter below can match a statement whatever case it was written in. */
    private static volatile String[] protectedEntityNamesLowerCase = new String[0];

    /**
     * @param protectedEntityNames the entity names whose writes must be authorized — used verbatim as a
     *                             cheap textual pre-filter, so they must be the names as they appear in
     *                             DQL, not table names
     */
    public static void registerWriteAuthorizer(WriteAuthorizer writeAuthorizer, String... protectedEntityNames) {
        String[] lowerCase = new String[protectedEntityNames.length];
        for (int i = 0; i < protectedEntityNames.length; i++)
            lowerCase[i] = protectedEntityNames[i].toLowerCase();
        protectedEntityNamesLowerCase = lowerCase;
        authorizer = writeAuthorizer;
    }

    public static boolean hasAuthorizer() {
        return authorizer != null;
    }

    /**
     * Registers something to be told when a protected write SUCCEEDS.
     *
     * <p>Separate from the authorizer because it answers a different question at a different moment:
     * the authorizer decides beforehand whether a write may happen, this reports afterwards that one
     * did. The reason it exists is caching — a rule that has just changed is exactly the rule a cached
     * decision is now wrong about, and the only component that knows the change happened is the one
     * that let it through.
     */
    public static void registerWriteObserver(WriteObserver writeObserver) {
        observer = writeObserver;
    }

    /** Never throws into the caller: a write that succeeded must not be reported as failed by its own bookkeeping. */
    public static void notifyWriteSucceeded(String entityName, WriteVerb verb) {
        WriteObserver currentObserver = observer;
        if (currentObserver != null) {
            try {
                currentObserver.onProtectedWriteSucceeded(entityName, verb);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /**
     * A cheap substring test answering "is it even worth parsing this statement?".
     *
     * <p>The authorization decision needs the parsed entity, but parsing every write to discover that
     * almost none of them touch a protected entity would put a parse on the hot path to protect a
     * handful of statements. A false positive here costs one parse; a false negative would be a hole,
     * so this errs by matching too much: any statement merely MENTIONING a protected name is parsed.
     */
    public static boolean mayTouchProtectedEntity(String dqlStatement) {
        if (authorizer == null || dqlStatement == null)
            return false;
        String lowerCase = dqlStatement.toLowerCase();
        for (String name : protectedEntityNamesLowerCase)
            if (lowerCase.contains(name))
                return true;
        return false;
    }

    /**
     * Asks the application, and fails the returned future when the answer is anything but yes.
     *
     * <p>Failing rather than returning false means a denied write propagates as an error the caller
     * already knows how to surface, and — more to the point — cannot be mistaken for a successful
     * no-op by a code path that forgot to inspect a boolean.
     */
    public static Future<Void> checkWriteAllowed(WriteRequest request) {
        WriteAuthorizer currentAuthorizer = authorizer;
        if (currentAuthorizer == null)
            return Future.succeededFuture();
        return currentAuthorizer.isWriteAuthorized(request)
            .otherwise(false) // an authorizer that fails denies; it does not abstain
            .compose(authorized -> Boolean.TRUE.equals(authorized)
                ? Future.succeededFuture()
                : Future.failedFuture("[NotAuthorizedError] Not authorized to " + request.verb() + " " + request.entityName()));
    }
}
