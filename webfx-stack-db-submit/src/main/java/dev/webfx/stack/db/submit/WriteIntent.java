package dev.webfx.stack.db.submit;

/**
 * What a write says it is doing, and where: the operation its caller believes they are performing, and
 * the context they are performing it in.
 *
 * <p>Travels with a {@link SubmitArgument} and is <b>untrusted</b>. The authorization request a server
 * judges is still built on the server from the statement; this is a SEPARATE input, which a server may
 * use in three ways of increasing strength — to narrow a grant held in several contexts, to verify
 * against the target's real owner and log a mismatch, or to constrain the statement so it cannot leave
 * the declared context. Only the third makes the claim a restriction rather than a permission, and
 * none of them lets a declaration widen what its caller may do.
 *
 * <h3>Why the context is three-valued, and why absent is not one of the three</h3>
 *
 * A write may belong to one organization, to one organization and event, or to <b>nothing</b> — and
 * the third is not a degenerate case. The authorization tables themselves have no organization and no
 * event column: the organization enters only through the access row. So the writes with no context at
 * all are the authorization configuration.
 *
 * <p>Which gives this class its shape: <b>an absent context must be a refusal, never a promotion.</b>
 * If "nothing declared" read as "global", the laziest call site would become the most privileged. So
 * there is no public constructor and no setter — a caller names one of the three cases through a
 * factory, and the ambiguous value (no organization, not global) cannot be built.
 *
 * @author Bruno Salmon
 */
public final class WriteIntent {

    private final String operationCode;
    private final Object organizationId;
    private final Object eventId;
    private final boolean global;

    /**
     * ONE constructor, deliberately, and private.
     *
     * <p>Private because the four fields admit a state no caller should be able to name — neither an
     * organization nor global, which is the absent context this class exists to keep out. One because
     * this module is compiled to JavaScript for both clients, and more than one constructor on a class
     * of this shape has crashed the GWT compiler while it computes permutations before now.
     */
    private WriteIntent(String operationCode, Object organizationId, Object eventId, boolean global) {
        this.operationCode = operationCode;
        this.organizationId = organizationId;
        this.eventId = eventId;
        this.global = global;
    }

    /** This operation, on rows belonging to this organization. */
    public static WriteIntent inOrganization(String operationCode, Object organizationId) {
        return new WriteIntent(operationCode, organizationId, null, false);
    }

    /** This operation, on rows belonging to this event of this organization. */
    public static WriteIntent inEvent(String operationCode, Object organizationId, Object eventId) {
        return new WriteIntent(operationCode, organizationId, eventId, false);
    }

    /**
     * This operation, on rows belonging to no organization at all — the authorization configuration
     * and the tables like it.
     *
     * <p>Declared explicitly, and authorized by a grant that is itself global, so that the hardest
     * claim to hold is also the one a caller has to say out loud.
     */
    public static WriteIntent global(String operationCode) {
        return new WriteIntent(operationCode, null, null, true);
    }

    /**
     * Rebuilds a decoded intent, including shapes a factory refuses.
     *
     * <p>For the codec only: a peer may send anything, and a decoder that silently repaired a
     * malformed claim would hide the one thing worth seeing. A claim with no context arrives as what
     * it is — {@link #hasContext()} false — and is refused by whoever judges it, not here.
     */
    public static WriteIntent decoded(String operationCode, Object organizationId, Object eventId, boolean global) {
        return new WriteIntent(operationCode, organizationId, eventId, global);
    }

    /** The operation the caller says this write performs, or null when it declared none. */
    public String getOperationCode() {
        return operationCode;
    }

    /** The organization the caller says this write belongs to, or null when global or undeclared. */
    public Object getOrganizationId() {
        return organizationId;
    }

    /** The event within that organization, or null when the claim is not event-scoped. */
    public Object getEventId() {
        return eventId;
    }

    /** Whether the claim is explicitly global — belonging to no organization. */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Whether a context was declared at all.
     *
     * <p>False is the refusable state, and the reason this is asked rather than inferred from a null
     * organization: global is also a null organization, and the two must never read alike.
     */
    public boolean hasContext() {
        return global || organizationId != null;
    }

    /**
     * How this reads in a log line.
     *
     * <p>Named ids, unlike the write inventory's counted contexts: this is the caller's own CLAIM about
     * where it is writing, not a fact derived from a row, and a mismatch between a claim and a resolved
     * owner is unreadable without both sides of it. An organization id is a reference to a centre
     * rather than to a person.
     */
    @Override
    public String toString() {
        if (operationCode == null)
            return "no-intent";
        if (global)
            return operationCode + "@global";
        if (organizationId == null)
            return operationCode + "@NO-CONTEXT";
        return operationCode + "@organizationId=" + organizationId
               + (eventId == null ? "" : ",eventId=" + eventId);
    }
}
