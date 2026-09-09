package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;

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

    /** What a family turned out to allow. Only {@link Verdict#RENEWED} and {@link Verdict#CURRENT} carry a usable generation. */
    enum Verdict {
        /** The presented generation was current; it has been retired and the family advanced. */
        RENEWED,
        /**
         * The presented generation was retired moments ago, inside the reuse grace. Almost always a
         * second tab that lost the race, so the family is NOT killed and the caller is handed the
         * current generation instead — see {@link SessionLifetime#REUSE_GRACE_MILLIS}.
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
