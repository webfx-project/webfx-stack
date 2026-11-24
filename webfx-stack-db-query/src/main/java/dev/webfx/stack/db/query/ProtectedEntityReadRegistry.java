package dev.webfx.stack.db.query;

/**
 * Where an application declares which client READS it refuses, and the framework holds none of that itself.
 *
 * <p>The twin of {@code ProtectedEntityWriteRegistry}, with the same registry inversion and the same rule:
 * this module can see the statement and cannot see the caller, the application can see the caller and should
 * not have to parse DQL, so the facts come from here and the verdict comes from there. There is no leniency
 * knob, because a knob is the thing an incident is later found to have been turned.
 *
 * <p><b>This is NOT {@link ClientReadInspectionRegistry}, and the split is deliberate.</b> That one observes
 * and is forbidden to refuse — step 0 learned that an observer wearing the authorizer's clothes starts
 * refusing unparseable statements the moment observation is switched on, and observation that can refuse is
 * not observation. This one refuses and does not observe. A deployment may run either, both or neither.
 *
 * <h3>A read has no target, and that is normal</h3>
 *
 * <p>The write side's {@code targetId} semantics do <b>not</b> carry over, and assuming they do is the first
 * mistake available here. For a write, a null target means "this could not be read" and a policy must deny:
 * the statement whose target is unreadable is precisely the one that reaches somebody else's row. For a read
 * there is usually no target at all and nothing is wrong — a list query is the ordinary case, not the
 * suspicious one. What a read must assert is not a known target but a <b>present binding</b>.
 *
 * <h3>Root entity and touched tables are BOTH carried, and choosing wrongly is a bug in each direction</h3>
 *
 * <p>They answer different questions and a policy must pick deliberately:
 *
 * <ul>
 * <li><b>{@link ReadRequest#touchedTables()} — what the statement REACHES.</b> Recorded from the compiler's
 *     own requests, never from the parsed text, because a DQL statement can reach a column without naming
 *     its table: {@code select person.firstName, person.email from Document} names one table and reads
 *     from two, returning names and addresses. A rule keyed on the root entity waves it through, and the
 *     reach can be longer than one hop — {@code documentLine.document.person.firstName from Attendance},
 *     since {@code Attendance} itself carries no {@code person}.
 *     This is the right question for a per-entity catalogue, and it is the same source
 *     {@link ClientReadDenyList} reads for the same reason.</li>
 * <li><b>{@link ReadRequest#entityName()} — what the statement is ROOTED at.</b> The right question when a
 *     join to the protected table is legitimate. Measured on production: anonymous callers read
 *     {@code Document scope=UNSCOPED … tables=[document, money_transfer, person]} — resume-payment showing
 *     the booker's name through a join. An arm refusing {@code person} BY TABLE would break the page that
 *     finding 6 closed, while refusing root {@code Person} selects closes the email oracle and touches
 *     nothing else.</li>
 * </ul>
 *
 * <p>So: the table for "may this caller see these columns at all", the root for "may this caller run this
 * KIND of query". Neither is the safe default; the policy states which it meant.
 *
 * <h3>Fail closed, and say so once</h3>
 *
 * <p>A policy that throws refuses, exactly as the write side's authorizer does. A policy that cannot answer
 * has not said yes, and a read path that treats silence as consent is a read path with an off switch in it.
 *
 * @author Claude Code
 */
public final class ProtectedEntityReadRegistry {

    /**
     * What one client read does, as the compiler saw it — the facts a policy decides on.
     *
     * @param entityName    the entity the statement is ROOTED at. See the class javadoc before keying a rule
     *                      on this rather than on {@code touchedTables}.
     * @param touchedTables every table the compiler asked the model for, which is what the statement really
     *                      reaches. Not the tables its text names, and not only its root's.
     * @param boundFields   the fields tied to a value by an equality or an {@code in}. What a read asserts
     *                      instead of a target id: the bound field is the binding, and its presence is what
     *                      a rule requiring one looks for.
     * @param bounded       whether every branch ties some column to a value. A statement that is not bounded
     *                      asks for the table.
     * @param hasWhere      whether every branch has a WHERE at all. Kept apart from {@code bounded} because
     *                      {@code where true} has one and guarantees nothing.
     */
    public record ReadRequest(
        String entityName,
        String[] touchedTables,
        String[] boundFields,
        boolean bounded,
        boolean hasWhere
    ) {
        // NO SECOND CONSTRUCTOR. A convenience overload defaulting the arrays would save callers a pair of
        // empties, and it crashes the GWT compiler while it computes permutations — this module is compiled
        // to JavaScript for both clients, and a record with more than one constructor is a shape it cannot
        // handle. The same note stands on ReadShape and on WriteRequest, for the same reason.
    }

    /** Declares which reads an application refuses. Asked on the caller's thread, before any async hop. */
    @FunctionalInterface
    public interface ReadPolicy {
        /**
         * @return why this read must not run, or null when it may. Called synchronously on the caller's own
         *         thread so that an implementation may read request-scoped state — the origin, the principal,
         *         the resolved scope — that would be gone after the first async hop. It must not await
         *         anything; the scope it needs is computed when a caller's grants are, and looked up here.
         */
        String refusalReason(ReadRequest request);
    }

    private static volatile ReadPolicy policy;

    private ProtectedEntityReadRegistry() {}

    /** Installs the application's read policy. Last registration wins, as on the write side. */
    public static void registerReadPolicy(ReadPolicy readPolicy) {
        policy = readPolicy;
    }

    /** Whether anything is declared. A caller uses this to tell "allowed" from "nobody asked". */
    public static boolean hasPolicy() {
        return policy != null;
    }

    /**
     * Why this read must not run, or null when it may — including when no policy is registered, which is the
     * only case where silence means consent and it means it because nothing has been declared.
     */
    public static String refusalReason(ReadRequest request) {
        ReadPolicy p = policy;
        if (p == null)
            return null;
        if (request == null)
            // Nothing to judge, and the caller could not describe its own statement: the shape a rule exists
            // to catch is likelier here than anywhere else.
            return ClientQueryGuard.UNCHECKABLE_REFUSED;
        try {
            return p.refusalReason(request);
        } catch (Throwable policyFailed) {
            // A policy that throws has not said yes. Matches ClientSubmitGuard, where an inspector that
            // throws refuses rather than abstains.
            return ClientQueryGuard.UNCHECKABLE_REFUSED;
        }
    }
}
