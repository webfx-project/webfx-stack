package dev.webfx.stack.db.query;

/**
 * Who wants to be told what clients READ, and what they are told.
 *
 * <p>Registry inversion, like {@code ProtectedEntityWriteRegistry} and {@code AuditActorRegistry}: the framework
 * owns one point every client read passes ({@link ClientQueryGuard}) and the application owns the policy — here,
 * the policy of what is worth looking at and what a look means.
 *
 * <h3>This cannot refuse anything, and that is the point</h3>
 *
 * <p>The write side learned this the expensive way, and its own comment says it: an observer wearing the
 * authorizer's clothes inherits the authorizer's fail-closed rules, so the day somebody switches observation on,
 * unparseable statements start being REFUSED. <b>Observation that can refuse is not observation.</b> So this is a
 * separate registry from {@link ClientQueryGuard}'s {@link ClientQueryGuard.Inspector}, which decides; nothing
 * here returns a verdict, and the guard runs its decision whether or not anyone is watching.
 *
 * <p>It is step 0 of the read-authorization plan ({@code docs/security/read-authorization-plan.md}): learn what
 * the clients actually read before writing the rule that constrains the rest. A default-deny needs an allowlist,
 * and an allowlist built by reading the front-end source is a list of what the code CAN do rather than what it
 * does — it misses whatever is composed at runtime, and it includes call sites nobody reaches any more.
 *
 * <h3>The division of labour, which is the same one the write side draws</h3>
 *
 * <p>The framework can see the STATEMENT and nothing else: it has no idea who sent it, and it must not learn.
 * The application can see the caller, and it is also the only side that knows what an ownership predicate looks
 * like in this domain — {@code accountCanAccessPersonOrders} is a Modality name and this module must never hold
 * it. So a {@link ReadShape} reports FACTS about a statement and passes no judgement: which entity it is rooted
 * at, which tables it reaches, which fields its WHERE binds, which functions it uses as guards. Whether that
 * adds up to "scoped to its caller" is the application's question to answer.
 *
 * <p>{@link ReadInspector#isInspecting()} is asked first, synchronously, before anything is parsed — which is
 * what keeps "only client traffic" expressible without this module learning what a client is.
 *
 * <h3>Cost, stated plainly because it is the reason this is opt-in</h3>
 *
 * <p>While an inspector is inspecting, a client statement not yet seen is parsed and compiled ONE EXTRA TIME, to
 * derive its shape by a route that cannot affect the verdict. Shapes are cached by statement text, so the steady
 * state is a map lookup per query. With no inspector registered, or one that says no, a read pays one volatile
 * read.
 *
 * @author Claude Code
 */
public final class ClientReadInspectionRegistry {

    private ClientReadInspectionRegistry() {}

    /**
     * What one client read looks like, in facts rather than verdicts.
     *
     * <p><b>Nothing here is anybody's data</b>, and that is a property to preserve rather than a coincidence.
     * Every field is a NAME — of an entity, a table, a field path, a function — taken from the domain model or
     * from the statement's structure. No parameter value and no literal reaches this record, so an inventory
     * built from it cannot become a store of personal data that the erasure path does not reach. A statement's
     * literals are where an email address or a surname would be; see {@link ClientQueryGuard#maskLiterals}, which
     * is what the statement text is put through before it is reported at all.
     *
     * @param entityName    the root domain class, or null when it could not be resolved
     * @param statementKind {@code "select"}, {@code "union"} or {@code "with"}. A union's branches are all
     *                      described and then INTERSECTED, so the facts below are what every branch guarantees;
     *                      a CTE is described from its main select, which is the only place its rows are
     *                      constrained on the way out
     * @param touchedTables every table the COMPILED SQL names — the FROM, every join, every table reached
     *                      through a dot path from an unrelated root, and the tables a bare foreign key pulls in
     *                      by loading its target's default fields. Taken from the compiler's own requests for the
     *                      domain model rather than from the statement's text, for the reason the denying reader
     *                      gives: what reaches SQL is the only thing that cannot be rephrased
     * @param boundFields   the field paths tied to a value in EVERY alternative — an equality or an IN, with a
     *                      column on one side and a literal or a {@code $n} on the other.
     *                      <p><b>A bound field is not a scoped query.</b> {@code where removed = $1} appears here
     *                      and matches nearly every row. This says which columns COULD carry a scope, not that
     *                      any of them does
     * @param alternativeFields the field paths tied to a value in at least one alternative. Read together with
     *                      {@code bounded}: where that is true and {@code boundFields} is empty, these are what
     *                      the statement is scoped by, spread across its branches. The front office's orders
     *                      union is exactly that — one branch binds {@code person.frontendAccount} and the other
     *                      {@code person.accountPerson.frontendAccount}, so no field is bound in both and the
     *                      statement is scoped to one account all the same
     * @param bounded       whether the rows are constrained at all. <b>Not derivable from {@code boundFields}</b>,
     *                      which is the whole reason it is here: two branches can each bind and share no field.
     *                      An AND is bounded when either side binds; an OR or a union only when every side does
     * @param guardFunctions the names of functions used as a boolean CONJUNCT — the position a domain-specific
     *                      ownership predicate occupies — holding on EVERY alternative. A function name appearing
     *                      in an argument, in only one side of an OR, in one branch of a union, or in the select
     *                      list is not here, because none of those restricts every row returned. Reported
     *                      unjudged: this module does not know which names mean ownership, and should not
     * @param constructs    the DISTINCT expression kinds the statement uses, by their term class name, from a
     *                      walk of every clause. Not a judgement: the point is to learn which of the grammar's
     *                      ~59 term classes clients actually send, so that a restricted dialect can be defined
     *                      from observation rather than from reading the front-end source. A class this walk
     *                      does not recognise is RECORDED rather than refused — an unanticipated construct is
     *                      exactly what this exists to surface, and the only place in this work where "unknown"
     *                      is data rather than a refusal.
     *                      <p>Static extraction cannot answer the same question: it misses everything composed
     *                      at runtime, and it cannot see the Java clients at all, which build statements
     *                      programmatically rather than as text
     * @param hasWhere      whether every branch has a WHERE at all. Separate from {@code bounded} on purpose:
     *                      {@code where true} has a WHERE and guarantees nothing, and an inventory describing
     *                      what clients send is worth telling those apart
     */
    public record ReadShape(
        String entityName,
        String statementKind,
        String[] touchedTables,
        String[] boundFields,
        String[] alternativeFields,
        String[] guardFunctions,
        String[] constructs,
        boolean bounded,
        boolean hasWhere
    ) {
        // NO SECOND CONSTRUCTOR. A convenience overload would save callers a couple of empty arrays, and it
        // crashes the GWT compiler while it computes permutations — this module is compiled to JavaScript for
        // both clients, and a record with more than one constructor is a shape it cannot handle.
    }

    /** Told what clients read. Cannot refuse, cannot change a verdict, must not throw. */
    public interface ReadInspector {

        /**
         * A client read a flat-watched column. Default does nothing, so an inspector written before flat
         * watches existed keeps compiling and simply reports nothing about them.
         */
        default void onObservedColumnRead(String statement, String[] columns) {}

        /**
         * Asked before the statement is parsed: is this caller's read one you want reported?
         *
         * <p>Called on the caller's own thread, before any async hop, so an implementation may read
         * request-scoped state — the origin, the principal — that would be gone a moment later.
         */
        boolean isInspecting();

        /** One read, described. Cannot refuse it. */
        void onRead(ReadShape shape);

        /**
         * A client read a column declared with {@code observeColumnExceptEqualityMatch} — watched, not refused.
         *
         * <p>What the enforced rule would have refused, reported instead. It exists so that restoring the
         * enforced rule can be a measurement rather than a date: the refusal used to BE the detection, so
         * pausing one removed the only way to tell whether the clients still sending the old statement had
         * gone, and switching it back on to find out costs another round of broken links.
         *
         * <p>The statement arrives with its literals already masked, and the columns are names out of the
         * domain model. A capability token is a secret, and the point of this is to stop handing it out — so
         * the VALUE never reaches here, only the fact that somebody read one.
         *
         * @param maskedStatement the statement, literals masked
         * @param columns         the watched columns the compiled SQL reached, as {@code table.column}
         * @param unanalysable    the walk met a construct it does not recognise, so this is "cannot say"
         *                        rather than "was read". Kept apart because they mean opposite things to the
         *                        decision this feeds: a read says wait, an unanalysable statement says look at
         *                        it. Lumped together, one unhandled construct pins the count above zero for
         *                        ever and the enforced rule can never be restored
         */
        default void onObservedCapabilityColumnRead(String maskedStatement, String[] columns, boolean unanalysable) {}

        /**
         * A statement the inspector was interested in and that could not be described — it did not parse, it did
         * not compile, or it was not a select at all.
         *
         * <p>Worth reporting rather than dropping: the guard refuses such a statement, so it never becomes a
         * shape, and an inventory that silently omitted them would describe a client's traffic as tidier than it
         * is. The number of them is what says whether this inventory covers all of the traffic or most of it.
         *
         * <p>The statement arrives with its literals already masked, by the framework rather than by trust: the
         * text is the sender's own, but a literal in it is where somebody's email address or surname would be.
         */
        default void onUndescribableStatement(String maskedStatement) {}
    }

    private static volatile ReadInspector inspector;

    /** Installs the inspector. Last registration wins; null removes it. */
    public static void registerReadInspector(ReadInspector readInspector) {
        inspector = readInspector;
    }

    /**
     * Whether this particular read should be described and reported — false whenever asking throws, so a broken
     * inspector observes nothing rather than failing the reads it was watching.
     */
    public static boolean isInspectingRead() {
        ReadInspector currentInspector = inspector;
        if (currentInspector == null)
            return false;
        try {
            return currentInspector.isInspecting();
        } catch (Throwable ignored) {
            // Throwable, not RuntimeException, and the breadth is the point: this is called OUTSIDE the
            // description's own catch, so anything escaping here reaches the guard — which treats a throwing
            // inspector as a refusal. An Error raised while deciding whether to WATCH a query would then refuse
            // it, which is the one thing this class promises cannot happen.
            return false;
        }
    }

    /** Never throws into the caller: watching a read must not be able to fail one. */
    public static void notifyRead(ReadShape shape) {
        ReadInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onRead(shape);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Never throws into the caller, for the same reason as {@link #notifyRead}. */
    public static void notifyObservedCapabilityColumnRead(String statement, String[] columns, boolean unanalysable) {
        ReadInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onObservedCapabilityColumnRead(
                    statement == null ? "(none)" : ClientQueryGuard.maskLiterals(statement), columns, unanalysable);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Told that a client read a FLAT-watched column — one that will be denied outright, with no equality use.
     *
     * <p>Unlike {@link #notifyObservedCapabilityColumnRead} there is no {@code unanalysable} here, and the
     * absence is the point: that flag exists because the capability walk can meet a construct it cannot read,
     * and "I could not tell" must not be counted as "a read". A flat column needs no walk — the compiler asked
     * for it, which IS the read — so there is no third answer to report.
     */
    public static void notifyObservedColumnRead(String statement, String[] columns) {
        ReadInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onObservedColumnRead(statement, columns);
            } catch (Throwable ignored) {
                // Watching a query must never be able to fail one.
            }
        }
    }

    /** Never throws into the caller, for the same reason as {@link #notifyRead}. */
    public static void notifyUndescribableStatement(String statement) {
        ReadInspector currentInspector = inspector;
        if (currentInspector != null) {
            try {
                currentInspector.onUndescribableStatement(
                    statement == null ? "(none)" : ClientQueryGuard.maskLiterals(statement));
            } catch (Throwable ignored) {
            }
        }
    }
}
