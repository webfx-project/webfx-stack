package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;

/**
 * Reads what has been revoked, page by page, and hands it to {@link RevokedFamilies}.
 *
 * <p>Here rather than beside the scheduler that drives it, because the arithmetic is where the mistakes
 * live and this way it can be exercised against a store that answers from memory — see RevocationPollCheck,
 * which caught both of the ones below. What the caller keeps is one method to call and one question to
 * ask: {@link #isCatchingUp()}, which is whether to come back in a second or in twenty.
 *
 * <h3>A cursor for progress</h3>
 *
 * <p>"Sign out everywhere" revokes every row in ONE statement, so thousands of families carry the same
 * timestamp to the microsecond. A bound that is only a time cannot walk through that: it reads the first
 * page, advances to that same timestamp, and reads the identical page again for ever, while the families
 * past the page boundary are never refused on sight at all. So reading is positional — strictly after the
 * row the last page ended on — which always advances, whatever the timestamps are.
 *
 * <h3>A window for what the cursor cannot see</h3>
 *
 * <p>A revocation is stamped when its UPDATE runs and becomes visible only when its transaction commits,
 * so one that commits slowly can appear BEHIND a cursor that has already passed it. A cursor alone would
 * never look back and would miss it until this instance next restarted.
 *
 * <p>So every so often one read covers the last few minutes instead, and — this is the part that was
 * wrong first — <b>it does not move the cursor</b>. Letting it would put the cursor back inside a bulk
 * revocation that is still inside the window, and the poll would walk those twelve thousand rows again,
 * every interval, for as long as the rows existed. It is a look aside, not a step back.
 *
 * <p>Everything here costs promptness and nothing else. A poll that throws, a page that never arrives, an
 * instance that has not polled since booting: the same tokens are refused at their next renewal, which is
 * the enforcement this only ever runs ahead of.
 *
 * @author Claude Code
 */
public final class RevocationPoll {

    private final SessionFamilyStore store;
    private final long startMillis;
    private final long safetyWindowMillis;
    private final int pollsBetweenSafetyReads;

    /** Where reading has got to. Null until the first page, which is what makes the first read a window. */
    private SessionFamilyStore.RevocationCursor cursor;

    /** The last page came back full, so the store has more and the caller should not wait its usual interval. */
    private boolean catchingUp;

    private int pollsSinceSafetyRead;

    /**
     * @param startMillis             where the first read begins — a retention window back at boot, so an
     *                                instance that started mid-incident learns what was revoked while it
     *                                did not exist rather than being the one task still honouring those
     *                                sessions
     * @param safetyWindowMillis      how far back the occasional look-aside reaches; a transaction that
     *                                takes longer than this to commit is caught at renewal instead
     * @param pollsBetweenSafetyReads how many ordinary polls pass between those look-asides
     */
    public RevocationPoll(SessionFamilyStore store, long startMillis, long safetyWindowMillis,
                          int pollsBetweenSafetyReads) {
        this.store = store;
        this.startMillis = startMillis;
        this.safetyWindowMillis = safetyWindowMillis;
        this.pollsBetweenSafetyReads = pollsBetweenSafetyReads;
    }

    /** Whether the last page came back full, meaning the store has more and the next poll should be soon. */
    public boolean isCatchingUp() {
        return catchingUp;
    }

    /**
     * Reads one page and reports how many families it newly marked as revoked — normally zero, because
     * nothing has been revoked in the last twenty seconds.
     */
    public Future<Integer> pollOnce(long nowMillis) {
        boolean firstRead = cursor == null;
        // A look-aside only when there is nothing to catch up on: while walking a bulk revocation, keep
        // walking it. It resumes afterwards, and the window is measured from now, so by then the bulk is
        // usually behind it anyway.
        boolean safetyRead = !firstRead && !catchingUp && ++pollsSinceSafetyRead >= pollsBetweenSafetyReads;
        long since = firstRead ? startMillis : nowMillis - safetyWindowMillis;
        SessionFamilyStore.RevocationCursor after = firstRead || safetyRead ? null : cursor;
        return store.revokedSince(since, after)
            .map(page -> {
                int added = RevokedFamilies.noteAll(page.revocations());
                if (safetyRead) {
                    // Deliberately touches neither the cursor nor catchingUp: see the class note.
                    pollsSinceSafetyRead = 0;
                } else {
                    if (page.next() != null)
                        cursor = page.next();
                    catchingUp = page.isFull();
                }
                RevokedFamilies.forgetOlderThan(nowMillis - RevokedFamilies.RETENTION_MILLIS);
                // Only on the way through a SUCCESSFUL read: the age of this is how an operator sees
                // that an instance has stopped hearing about revocations performed on the other one.
                RevokedFamilies.notePollSucceeded(nowMillis);
                return added;
            });
    }
}
