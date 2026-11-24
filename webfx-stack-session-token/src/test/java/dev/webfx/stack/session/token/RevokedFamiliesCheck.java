package dev.webfx.stack.session.token;

import java.util.List;

/**
 * The set that makes a revocation bite on the next message instead of at the next renewal.
 *
 * <p>Two properties are worth holding on to, and they pull in opposite directions. It must SAY YES for a
 * family somebody has revoked, because a thief who never races anybody is invisible to rotation and this
 * is the only thing that refuses them promptly. And it must be free to say NO — for a revocation it has
 * not polled yet, for one it has aged out, for everything after a restart — because it is consulted on
 * every message and nothing may depend on it being complete. Renewal is the enforcement; this is latency.
 *
 * <p>So the cases below check what it remembers, what it deliberately forgets, and that forgetting is
 * decided by WHEN a family was revoked rather than when it was heard about — an entry learned late from a
 * poll is not fresher for having arrived late.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class RevokedFamiliesCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    static SessionFamilyStore.Revocation revocation(String familyId, long atMillis) {
        return new SessionFamilyStore.Revocation(familyId, atMillis);
    }

    public static void main(String[] args) {
        long now = 1_800_000_000_000L;
        RevokedFamilies.clear();

        System.out.println("what it says before it has been told anything:");
        check("an unknown family is not revoked", !RevokedFamilies.isRevoked("fam-1"));
        check("and neither is a token that names no family at all", !RevokedFamilies.isRevoked(null));

        System.out.println("what it remembers:");
        RevokedFamilies.note("fam-1", now);
        check("the family just revoked here", RevokedFamilies.isRevoked("fam-1"));
        check("and still only that one", !RevokedFamilies.isRevoked("fam-2") && RevokedFamilies.size() == 1);

        System.out.println("a poll re-reads its own window, so only what is NEW counts:");
        int added = RevokedFamilies.noteAll(List.of(revocation("fam-1", now), revocation("fam-2", now)));
        check("one of the two was new", added == 1);
        check("both are refused either way", RevokedFamilies.isRevoked("fam-1") && RevokedFamilies.isRevoked("fam-2"));
        check("and noting the same one again adds nothing",
              RevokedFamilies.noteAll(List.of(revocation("fam-2", now))) == 0);

        System.out.println("what it forgets, and on what clock:");
        RevokedFamilies.clear();
        long old = now - RevokedFamilies.RETENTION_MILLIS - 1;
        RevokedFamilies.note("ancient", old);
        RevokedFamilies.note("recent", now);
        RevokedFamilies.forgetOlderThan(now - RevokedFamilies.RETENTION_MILLIS);
        // Aged out, not lost: every token of that family has had to renew many times over by now, and
        // renewal refuses it from the store. Keeping it here would grow a year of logouts to answer a
        // question about the last few minutes.
        check("a revocation older than the retention is dropped", !RevokedFamilies.isRevoked("ancient"));
        check("a recent one is kept", RevokedFamilies.isRevoked("recent"));
        // The distinction that matters: an entry that arrived late from a poll is judged by when the
        // family was REVOKED, not by when this instance heard about it.
        RevokedFamilies.noteAll(List.of(revocation("heard-late", old)));
        RevokedFamilies.forgetOlderThan(now - RevokedFamilies.RETENTION_MILLIS);
        check("and learning of an old revocation late does not make it recent",
              !RevokedFamilies.isRevoked("heard-late"));

        System.out.println("a refusal is reported once, not once per message:");
        RevokedFamilies.clear();
        RevokedFamilies.note("noisy", now);
        check("the first refusal is worth a line", RevokedFamilies.shouldReportRefusal("noisy"));
        // A client that ignores the logout presents the same dead token for as long as it stays connected.
        check("the next thousand are not", !RevokedFamilies.shouldReportRefusal("noisy")
              && !RevokedFamilies.shouldReportRefusal("noisy"));
        check("and a family nobody revoked still reports, since there is nothing to have reported",
              RevokedFamilies.shouldReportRefusal("never-revoked"));

        System.out.println("it may be emptied at any time, because it is a cache and not a record:");
        RevokedFamilies.note("fam-3", now);
        RevokedFamilies.clear();
        check("nothing is held", RevokedFamilies.size() == 0);
        check("and a revoked family reads as unknown — refused later by renewal, not here",
              !RevokedFamilies.isRevoked("fam-3"));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
