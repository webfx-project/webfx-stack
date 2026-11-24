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

    /**
     * A written value the seam could not read — an expression rather than a literal or a parameter, a
     * parameter out of range, or a non-scalar. Compare by identity.
     *
     * <p>It used to be null, which made it indistinguishable from a field explicitly set to null. A rule
     * placing a row by a written owner field then read {@code event = 1897+1} as "no event", and judged the
     * write in the wrong place: the caller chose the reading by choosing to write an expression. So unknown
     * is now its own value, as a same-batch reference already was, and a rule that cannot use it refuses.
     */
    public static final Object UNRESOLVED_VALUE = new Object() {
        @Override
        public String toString() {
            return "(unresolved)";
        }
    };

    /** The three ways a DQL statement can change rows. */
    public enum WriteVerb { INSERT, UPDATE, DELETE }

    /**
     * Everything the seam could determine about one write.
     *
     * @param writtenFields the fields this statement SETS, empty for a delete
     * @param writtenValues those fields' values where they resolve to a scalar — an insert names its
     *                      owner here ({@code insert Document set person=$1}), which is how a new row's
     *                      ownership can be judged without a row existing to look up.
     *                      <p><b>Null means the value IS null</b> — a {@code null} literal or a null
     *                      parameter. A value this could not read (an expression such as
     *                      {@code event = 1897+1}, a parameter out of range, a non-scalar) is
     *                      {@link #UNRESOLVED_VALUE}, and a {@link GeneratedKeyReference} is a row this
     *                      batch creates. A rule must never read either as null: "this field was cleared"
     *                      and "this field was set to something I cannot see" are opposite answers.
     * @param targetId      the id of the row being changed, when an {@code id = value} test can be read
     *                      out of the WHERE — either as the whole of it, or as one side of a conjunction
     *                      — and <b>null when it could not be determined</b>, which covers every other
     *                      shape: a disjunction, a subquery, a non-id predicate.
     *                      <p><b>Null is not "no constraint", it is "unknown".</b> A policy that decides
     *                      by ownership must DENY on null: a statement whose target this could not read
     *                      is precisely the statement that would be used to reach somebody else's row.
     *                      Reading null as "not applicable" would make the check optional at the
     *                      attacker's discretion.
     * @param unbounded     true when this write names NO bound on which rows it touches: an update or a
     *                      delete with no WHERE, or whose WHERE holds no equality or IN tying a column
     *                      to a literal or a {@code $n}. {@code where true}, {@code where id = id} and
     *                      {@code where id > 0} are all unbounded. Always false for an insert.
     *                      <p>Distinct from a null {@code targetId}, and the difference is the one that
     *                      matters: null there means "this could not be read", which is true of most
     *                      set-based writes, including perfectly ordinary ones like
     *                      {@code delete from ListItem where list = $1}. A rule built on that null
     *                      refuses the honest statements along with the rest.
     *                      <p><b>Bounded is not safe.</b> {@code where removed = $1} is bounded by this
     *                      and can still match nearly every row. It is a floor under the statement that
     *                      names no bound at all, not an answer to whose rows these are.
     * @param batchWrites   the inspected write of each statement of the enclosing batch, null at a statement
     *                      that inserts nothing and null entirely when this write was judged outside a batch.
     *                      Read through {@link #batchInsertAt} or {@link #batchWriteAt} rather than directly.
     *                      <p><b>Shared, and not copied.</b> A record does not copy an array component, so
     *                      every write of the batch holds THIS array: a rule that sorted it, cleared an entry
     *                      or wrote into it would change what every policy after it sees, and what the writes
     *                      already judged were judged against. It is the batch's, not the rule's — read it.
     */
    public record WriteRequest(
        String entityName,
        WriteVerb verb,
        String[] writtenFields,
        Map<String, Object> writtenValues,
        Object targetId,
        boolean unbounded,
        WriteRequest[] batchWrites
    ) {

        // NO SECOND CONSTRUCTOR. A convenience overload defaulting batchWrites to null would save the
        // callers a `null` each, and it crashes the GWT compiler while it computes permutations — this
        // module is compiled to JavaScript for both clients, and a record with more than one
        // constructor is a shape it cannot handle. The callers pass null explicitly instead, which also
        // makes "this write was judged outside a batch" something a reader sees at the call site.

        /** The same write, told what the batch around it inserts. */
        public WriteRequest withBatchWrites(WriteRequest[] batchWrites) {
            return batchWrites == this.batchWrites ? this // the single-submit path, where both are null
                : new WriteRequest(entityName, verb, writtenFields, writtenValues, targetId, unbounded, batchWrites);
        }

        /**
         * What statement {@code index} of the enclosing batch INSERTS, or null when it inserts nothing, when the
         * index names no statement, or when this write was judged outside a batch.
         *
         * <p>This exists because a value can name a row the same batch is creating rather than one that already
         * exists — a {@code GeneratedKeyReference} — and a rule that accepts such a value has to know WHAT is
         * being created there. Reading only "it is a reference" says the caller is pointing at some statement of
         * its own batch, not that it is pointing at the kind of row the rule meant to allow; the batch is the
         * caller's to compose, so the statement pointed at can be anything it likes.
         *
         * <p><b>Null is "unknown", as everywhere else here.</b> A single-statement submit has no batch, so every
         * index is null and a rule that requires a particular entity refuses — which is right: a reference to a
         * batch that does not exist resolves to nothing.
         */
        public String batchInsertAt(int index) {
            WriteRequest insert = batchWriteAt(index);
            return insert == null ? null : insert.entityName();
        }

        /**
         * The whole write at statement {@code index} of the enclosing batch, under the same conditions and with
         * the same meaning for null as {@link #batchInsertAt}, which is derived from it.
         *
         * <p>The name alone answers "what kind of row is this reference pointing at". It cannot answer <b>where
         * that row will live</b>, and a rule that places a write has to: a booking is written as one batch, so a
         * line and its attendances reference a document that has no id yet, and following the reference to a
         * name leaves them in no context at all — where no organization-scoped grant can ever satisfy them.
         * The written values of the statement being pointed at are what resolve it, so the request carries them
         * rather than being re-read; the entity name is still what most rules want, so it keeps its own method.
         *
         * <p>The returned request does NOT itself carry the batch: the array holds the writes as they were
         * inspected, so there is no cycle to walk into, and a caller following a chain of references passes the
         * batch down itself (and bounds how far it will go — the batch is the caller's to compose).
         */
        public WriteRequest batchWriteAt(int index) {
            return batchWrites == null || index < 0 || index >= batchWrites.length ? null : batchWrites[index];
        }
    }

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

        /**
         * One submit is beginning: every {@link #onWrite} until {@link #onSubmitEnd} belongs to it.
         *
         * <p><b>Why the boundary is worth reporting at all.</b> A submit is one transaction, and the
         * question an inventory of writes cannot otherwise answer is what ONE of them contains — whether
         * a single flush spans two organizations, and whether each could name one operation. Per
         * statement, that is invisible: the shapes arrive in a stream with nothing saying where one
         * caller's action ended and the next began.
         *
         * <p>Reported rather than inferred for a reason. A batch's writes are all started synchronously
         * on the caller's own thread before any future completes, so an inspector CAN tell that
         * consecutive calls on one thread are related — but not where the run stops, which makes the
         * last submit of every thread unobservable and any timeout a guess. The interceptor knows
         * exactly, and saying so costs two calls.
         *
         * <p>Default no-op: an inspector that only wants shapes implements neither.
         *
         * @param statementCount how many statements the submit carries, including ones this inspector
         *                       will not be told about (not DQL, or naming no protected entity) — so an
         *                       inspector can tell a submit it saw whole from one it saw part of
         */
        default void onSubmitBegin(int statementCount) {}

        /**
         * What one statement of the open submit DECLARED it was doing — untrusted, and reported so that
         * a measurement can be taken before anything trusts it.
         *
         * <p>Reported per statement rather than per submit because the field is per statement, and
         * that is itself the thing being measured: whether the statements of one caller action all name
         * the same operation, or whether some actions are composite and must either split or keep the
         * per-entity map. An inspector is not told which write each declaration belongs to, because the
         * question is about the submit, not the statement — which also means no ordering between this
         * and {@link #onWrite} has to be relied on.
         */
        default void onDeclaredIntent(WriteIntent intent) {}

        /**
         * That submit is complete, whether or not it was authorized or executed.
         *
         * <p>Called from a finally, so a refusal or a throw between the two still closes the group: an
         * inspector that accumulated until the next begin would otherwise attribute the next caller's
         * writes to this one.
         */
        default void onSubmitEnd() {}
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

    /**
     * Opens a submit group on the inspector, if one is watching.
     *
     * <p>Asked whether anyone is inspecting at all before notifying: with no inspector this is two
     * volatile reads per submit and nothing else.
     */
    public static void notifySubmitBegin(int statementCount) {
        WriteInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onSubmitBegin(statementCount);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Never throws into the caller, for the same reason as {@link #notifyWriteInspected}. */
    public static void notifyDeclaredIntent(WriteIntent intent) {
        WriteInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onDeclaredIntent(intent);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Closes it. Never throws into the caller, for the same reason as {@link #notifyWriteInspected}. */
    public static void notifySubmitEnd() {
        WriteInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onSubmitEnd();
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
