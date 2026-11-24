package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reading revocations back, including the case that made the first version of this loop useless.
 *
 * <p><b>"Sign out everywhere" revokes every row in one statement</b>, so thousands of families carry the
 * same timestamp to the microsecond. A poll bounded only by time then reads the first page, advances its
 * watermark to that same timestamp, reaches back over it for safety, and reads the identical page again —
 * for ever, at one page a second, while the families past the page boundary are never refused on sight at
 * all. Silently, because every log line says it is working.
 *
 * <p>So the cases below walk a bulk revocation to its end, and then check the other half: that after
 * catching up the poll goes back to a WINDOW that overlaps what it already read, because a transaction
 * can commit late and surface a revocation timestamped behind the watermark. The two bounds cover
 * different failures and neither alone is enough.
 *
 * <p>The store here orders and pages exactly as the SQL does — ordered by (revoked, id), cursor strictly
 * after the last row of the previous page — so that the contract the interface states is exercised by
 * something other than the one implementation that must obey it.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class RevocationPollCheck {

    static int pass = 0, fail = 0;

    static final long NOW = 1_800_000_000_000L;
    static final long SAFETY_WINDOW = 5 * 60_000L;
    static final int POLLS_BETWEEN_SAFETY_READS = 15;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** A stamp that sorts the way a timestamp does, which is all the cursor needs of it. */
    static String stampOf(long millis) {
        return String.format("%019d", millis * 1000); // microseconds, as the real column holds
    }

    /** Pages the way the two SQL statements do, and records how it was asked and how much it served. */
    static class PagingStore implements SessionFamilyStore {
        final List<Revocation> revoked = new ArrayList<>();
        final List<String> asked = new ArrayList<>();
        int rowsServed;

        void revoke(String familyId, long atMillis) {
            revoked.add(new Revocation(familyId, atMillis));
        }

        @Override
        public Future<RevocationPage> revokedSince(long sinceMillis, RevocationCursor after) {
            asked.add(after == null ? "window:" + sinceMillis : "cursor:" + after.familyId());
            List<Revocation> ordered = new ArrayList<>(revoked);
            ordered.sort(Comparator.comparingLong(Revocation::revokedAtMillis).thenComparing(Revocation::familyId));
            List<Revocation> page = new ArrayList<>();
            RevocationCursor next = null;
            for (Revocation revocation : ordered) {
                boolean included = after == null
                                   ? revocation.revokedAtMillis() > sinceMillis
                                   : (stampOf(revocation.revokedAtMillis()) + revocation.familyId())
                                     .compareTo(after.stamp() + after.familyId()) > 0;
                if (!included)
                    continue;
                page.add(revocation);
                next = new RevocationCursor(stampOf(revocation.revokedAtMillis()), revocation.familyId());
                if (page.size() >= REVOCATION_PAGE_SIZE)
                    break;
            }
            rowsServed += page.size();
            return Future.succeededFuture(new RevocationPage(page, next));
        }

        @Override public Future<String> open(Object principal, SessionTier tier, long absoluteExpiryMillis) { return Future.failedFuture("unused"); }
        @Override public Future<FamilyRenewal> renew(String familyId, int presentedGeneration, long nowMillis) { return Future.failedFuture("unused"); }
        @Override public Future<Void> revoke(String familyId, String reason) { return Future.failedFuture("unused"); }
        @Override public Future<List<String>> revokeOtherFamilies(Object principal, String exceptFamilyId, String reason) { return Future.failedFuture("unused"); }
    }

    /** Polls until it says it has caught up, and answers how many pages that took. */
    static int pollToEnd(RevocationPoll poll) {
        int pages = 0;
        do {
            poll.pollOnce(NOW);
            pages++;
        } while (poll.isCatchingUp() && pages < 100); // the bound is what turns a stall into a failure
        return pages;
    }

    static RevocationPoll pollFor(PagingStore store, long startMillis) {
        return new RevocationPoll(store, startMillis, SAFETY_WINDOW, POLLS_BETWEEN_SAFETY_READS);
    }

    public static void main(String[] args) {
        System.out.println("a bulk revocation: 12,000 families, one statement, one timestamp:");
        RevokedFamilies.clear();
        PagingStore store = new PagingStore();
        long bulkAt = NOW - 5_000;
        for (int i = 1; i <= 12_000; i++)
            store.revoke("bulk-" + String.format("%05d", i), bulkAt);
        RevocationPoll poll = pollFor(store, NOW - 10_000);
        int pages = pollToEnd(poll);
        check("every one of them is refused on sight", RevokedFamilies.size() == 12_000);
        check("read in pages rather than in one result", pages == 3); // 5000 + 5000 + 2000
        check("and it knows it has finished", !poll.isCatchingUp());
        check("the pages after the first followed a CURSOR — a time bound alone would re-read page one",
              store.asked.size() == 3 && store.asked.get(1).startsWith("cursor:") && store.asked.get(2).startsWith("cursor:"));

        System.out.println("and then it stops reading them — no treadmill:");
        // From here the clock advances as the scheduler would, twenty seconds a poll, because the
        // look-aside window is measured from NOW: with time frozen a bulk revocation never leaves it.
        long clock = NOW;
        store.asked.clear();
        int servedAfterCatchUp = store.rowsServed;
        for (int i = 0; i < 10; i++)
            poll.pollOnce(clock += 20_000);
        check("ten further polls read nothing at all", store.rowsServed == servedAfterCatchUp);
        check("each asked by cursor", store.asked.stream().allMatch(a -> a.startsWith("cursor:")));

        System.out.println("every so often it looks aside, at the last few minutes:");
        store.asked.clear();
        long beforeLookAside = clock;
        for (int i = 0; i < POLLS_BETWEEN_SAFETY_READS; i++)
            poll.pollOnce(clock += 20_000);
        String window = store.asked.stream().filter(a -> a.startsWith("window:")).findFirst().orElse(null);
        check("one of those polls asked by window", window != null);
        long askedFrom = window == null ? 0 : Long.parseLong(window.substring("window:".length()));
        check("reaching back one safety window from the time it asked",
              askedFrom >= beforeLookAside - SAFETY_WINDOW && askedFrom <= clock - SAFETY_WINDOW);

        System.out.println("which is what catches a transaction that committed late:");
        // Stamped while the bulk ran — so behind the cursor — but only visible now.
        store.revoke("late-commit", clock - 60_000);
        for (int i = 0; i <= POLLS_BETWEEN_SAFETY_READS; i++)
            poll.pollOnce(clock += 20_000);
        check("the late revocation is picked up", RevokedFamilies.isRevoked("late-commit"));
        check("and nothing else changed", RevokedFamilies.size() == 12_001);

        System.out.println("a look-aside must not put the cursor back inside the bulk:");
        // Had it moved the cursor, every poll after it would walk those twelve thousand rows again. Once
        // the bulk is older than the look-aside window, nothing should read anything at all.
        clock += SAFETY_WINDOW;
        store.asked.clear();
        int servedBefore = store.rowsServed;
        for (int i = 0; i < 40; i++)
            poll.pollOnce(clock += 20_000);
        check("forty later polls read nothing", store.rowsServed == servedBefore);

        System.out.println("what a quiet poll does:");
        RevokedFamilies.clear();
        PagingStore quiet = new PagingStore();
        RevocationPoll quietPoll = pollFor(quiet, NOW - 10_000);
        quietPoll.pollOnce(NOW);
        check("nothing is held", RevokedFamilies.size() == 0);
        check("and nothing is being caught up on", !quietPoll.isCatchingUp());

        System.out.println("and what it forgets while it polls:");
        PagingStore ageing = new PagingStore();
        ageing.revoke("ancient", NOW - RevokedFamilies.RETENTION_MILLIS - 1);
        ageing.revoke("recent", NOW - 1_000);
        RevocationPoll ageingPoll = pollFor(ageing, NOW - RevokedFamilies.RETENTION_MILLIS - 10_000);
        ageingPoll.pollOnce(NOW);
        check("a revocation past the retention is not kept", !RevokedFamilies.isRevoked("ancient"));
        check("a recent one is", RevokedFamilies.isRevoked("recent"));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
