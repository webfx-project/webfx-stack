package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;

import java.util.List;
import java.util.Set;

/**
 * Where a session family's generation counter lives, so that a retired token can be recognised.
 *
 * <h3>Why a counter and not the tokens</h3>
 *
 * <p>Keeping issued or retired tokens grows without bound and puts credential-shaped material at rest
 * for no gain. A family gets a counter instead: the signed payload carries {@code (familyId, generation)},
 * renewal increments it, and on presentation {@code generation == current} is fine, {@code generation <
 * current} means a retired token is in somebody's hands, and {@code generation > current} cannot happen
 * without forging the MAC. One row per active session, nothing secret in it, bounded by concurrent
 * sessions rather than by renewals over time.
 *
 * <p>This is what converts a stolen token from an undetectable capability into a detectable event — the
 * highest-value thing on this whole piece of work that is not the identity flip itself, because it is a
 * compromise signal that today does not exist in any form.
 *
 * <h3>Two rules an implementation must not get wrong</h3>
 *
 * <p><b>An unreadable row is not a verdict.</b> If the database does not answer, fail the returned
 * future. Do NOT return a verdict that ends the session: treating "the database did not answer" as
 * "this token is retired" would turn a database blip into a mass logout, which is the same error as
 * recovering a failed identity check into a logged-out user, and a table on the authentication path is
 * exactly where that comes back.
 *
 * <p><b>The increment must be atomic against the live row.</b> Two instances share nothing but the
 * database during a blue/green deploy, and two browser tabs share one token. A read-then-write, or a
 * write whose condition is evaluated against a snapshot, lets both renewals succeed and leaves one
 * client holding a generation the other has retired — a suspected theft that is really a second tab.
 *
 * @author Claude Code
 */
public interface SessionFamilyStore {

    /**
     * Opens a family for a session that has just been established by a real credential check.
     *
     * @param principal            who the check established; an implementation may record identifiers
     *                             from it, but nothing here is secret and nothing has to be
     * @param tier                 the lifetime policy this session runs under
     * @param absoluteExpiryMillis the bound this session may never outlive, stored so it can be
     *                             SHORTENED from the server later — a cap frozen into a credential is a
     *                             cap nothing can reach
     * @return the new family's id, to be signed into the token
     */
    Future<String> open(Object principal, SessionTier tier, long absoluteExpiryMillis);

    /**
     * The same, also recording what the token's {@code $aud} and {@code $amr} will say.
     *
     * <p>A store that keeps these can answer what a log line cannot: not only which sessions ended
     * badly, but what share of the sessions opened the same way did — a rate is not a finding without
     * its denominator. Both are frozen at login and carried unchanged through every renewal, so they
     * are written once, here, and a row that later disagreed with its token would be a bug rather than
     * drift.
     *
     * <p>Defaults to discarding them, so a store that has nowhere to put them — every in-memory one in
     * the checks — needs no change. Null audience and empty methods are "not recorded", which is what a
     * legacy token being upgraded genuinely has.
     */
    default Future<String> open(Object principal, SessionTier tier, SessionAudience audience,
                                Set<SessionMethod> methods, long absoluteExpiryMillis) {
        return open(principal, tier, absoluteExpiryMillis);
    }

    /**
     * Advances a family by one generation, or explains why it will not.
     *
     * @param familyId          the family named by the presented token
     * @param presentedGeneration the generation that token carries
     * @return the verdict; a FAILED future means the store could not answer, which is not a verdict
     */
    Future<FamilyRenewal> renew(String familyId, int presentedGeneration, long nowMillis);

    /**
     * Ends a family permanently. Used when a retired token is presented — the copy is in somebody's
     * hands and which holder is the legitimate one cannot be known, so the family dies and everyone on
     * it logs in again.
     */
    Future<Void> revoke(String familyId, String reason);

    /**
     * Ends every live family of {@code principal} except {@code exceptFamilyId} — "sign out my other
     * devices", and later the half of the panic button that ends the sessions.
     *
     * <p>Answers with the ids it ended, which is what lets the caller refuse them on sight here rather
     * than waiting for its own poll to read back its own write. The list may be short of one that the
     * statement did end: a session opened between reading the ids and writing is revoked by the write
     * and not named in the answer. That costs it prompt refusal on this instance and nothing else — it
     * is refused at its next renewal like any other.
     *
     * <p>A principal with nobody behind it — a guest, whose sessions are reached through a booking link
     * rather than an account — has no other devices to sign out, and this must end nothing.
     */
    Future<List<String>> revokeOtherFamilies(Object principal, String exceptFamilyId, String reason);

    /**
     * When alarm mode lapses, or 0 when it is not raised — read on the same poll as revocations, and for
     * the same reason: instances do not hear each other, so the database is where they agree.
     *
     * <p>Here rather than in a store of its own because it is the same question at a different scale —
     * "which sessions should stop trusting themselves, and how soon" — read by the same task, on the same
     * tick, against the same datasource. A deployment whose store does not implement it simply never
     * raises the alarm, which is why this answers "none" by default rather than failing.
     *
     * @see SecurityAlarm
     */
    default Future<Long> alarmExpiryMillis() {
        return Future.succeededFuture(0L);
    }

    /**
     * Raises alarm mode until {@code expiryMillis}, or extends it to then — the store records the moment
     * it lapses, never a flag, so that it ends without anybody having to remember to end it.
     *
     * <p>Answers with the expiry actually in force afterwards, which is the later of what was asked for
     * and what was already there: a second raise must never SHORTEN an alarm somebody else has just
     * raised for longer.
     *
     * @param expiryMillis      when the alarm should lapse
     * @param raisedByPersonId  who raised it, for the record; may be null where the store keeps none
     */
    default Future<Long> raiseAlarm(long expiryMillis, Object raisedByPersonId) {
        return Future.failedFuture("This deployment does not record alarm mode");
    }

    /**
     * A page of revocations, so an instance can learn what the OTHER one revoked.
     *
     * <p>Polling rather than being told, because there is no clustered event bus — the same fact that
     * made the token self-contained. A revocation performed while handling a message on one instance is
     * invisible to the other, and during a blue/green deploy that other instance holds half the clients.
     *
     * <p>Two ways to ask, and the difference is what keeps a bulk revocation from stalling:
     *
     * <ul>
     *   <li><b>{@code after == null}</b> — everything revoked since {@code sinceMillis}. The caller sets
     *       that bound a little behind what it has already seen, because a revocation is stamped when its
     *       UPDATE runs and becomes visible only when its transaction commits, so a window starting
     *       exactly where the last one ended can step over a slow commit. Re-reading is free: noting a
     *       revocation twice changes nothing.</li>
     *   <li><b>{@code after != null}</b> — strictly after that cursor, which is where the previous full
     *       page ended. Needed because "sign out everywhere" revokes every row in ONE statement, so
     *       thousands of families share a single timestamp to the microsecond: a time-only bound would
     *       return the same first page forever and never reach the rest.</li>
     * </ul>
     *
     * <p>Ordered oldest first and capped at {@link #REVOCATION_PAGE_SIZE}, so a mass revocation is read in
     * pages rather than in one unbounded result.
     */
    Future<RevocationPage> revokedSince(long sinceMillis, RevocationCursor after);

    /**
     * The cap on one page. A bulk revocation — offboarding, or a member ending every session they have —
     * must not come back as one unbounded result.
     */
    int REVOCATION_PAGE_SIZE = 5_000;

    /** A revoked family, and when: the two things {@code RevokedFamilies} needs and nothing else. */
    record Revocation(String familyId, long revokedAtMillis) {}

    /**
     * Where a page ended, so the next one can resume strictly after it.
     *
     * <p>Opaque to whoever polls: the store writes it and the store reads it. It carries the revocation
     * time as the store's own TEXT rather than as milliseconds on purpose — milliseconds are a rounding
     * of a microsecond timestamp, and a cursor that rounded UP would skip the rows it was meant to
     * resume from, which is the very case this exists for.
     */
    record RevocationCursor(String stamp, String familyId) {}

    /** One page, and where it ended. {@link #isFull()} is how a caller knows there is more to read. */
    record RevocationPage(List<Revocation> revocations, RevocationCursor next) {

        public static final RevocationPage EMPTY = new RevocationPage(List.of(), null);

        public boolean isFull() {
            return revocations.size() >= REVOCATION_PAGE_SIZE;
        }
    }

    /** What a family turned out to allow. Only {@link Verdict#RENEWED} and {@link Verdict#CURRENT} carry a usable generation. */
    enum Verdict {
        /** The presented generation was current; it has been retired and the family advanced. */
        RENEWED,
        /**
         * The presented generation was retired moments ago, inside the reuse grace. Almost always a
         * second tab that lost the race, so the family is NOT killed and the caller is handed the
         * current generation instead: a holder exactly one generation behind is the only holder anybody
         * has heard from, since a generation advances only when the one before it is presented.
         */
        CURRENT,
        /** A generation retired long enough ago that a second holder is the likeliest explanation. */
        REUSE_DETECTED,
        /** Revoked, past its absolute bound, or a family this store has never heard of. */
        ENDED,
        /**
         * Nothing could be concluded — typically another renewal of the same family committed between
         * this one's read and its write. Distinct from a failure, and deliberately not folded into
         * either neighbour: guessing RENEWED would hand out a stale generation, and guessing ENDED would
         * sign someone out for a race. The caller keeps the session on its current token and tries again.
         */
        UNDECIDED
    }

    /**
     * @param generation           the generation the caller should now mint at; meaningless unless the
     *                             verdict is RENEWED or CURRENT
     * @param absoluteExpiryMillis the family's bound as the STORE holds it, which may have been
     *                             shortened since the token was minted
     */
    record FamilyRenewal(Verdict verdict, int generation, long absoluteExpiryMillis) {

        public static FamilyRenewal ended() {
            return new FamilyRenewal(Verdict.ENDED, 0, 0);
        }

        public static FamilyRenewal reuseDetected() {
            return new FamilyRenewal(Verdict.REUSE_DETECTED, 0, 0);
        }

        public static FamilyRenewal undecided() {
            return new FamilyRenewal(Verdict.UNDECIDED, 0, 0);
        }
    }
}
