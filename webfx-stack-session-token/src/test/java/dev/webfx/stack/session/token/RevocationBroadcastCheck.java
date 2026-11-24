package dev.webfx.stack.session.token;

import java.util.ArrayList;
import java.util.List;

/**
 * Who gets told a family has been revoked, and — the part that actually costs something — who does not.
 *
 * <p>The announcement exists so a revoked session's screen goes at once instead of at the next click. That
 * makes it a message to a live browser, and the failure modes are the ones messages have:
 *
 * <ul>
 *   <li><b>Saying it twice.</b> The poll re-reads a safety window every few minutes, so the same family is
 *       noted again and again. Announcing each time would push a logout at a client that logged out long
 *       ago, and bury the real announcement in the log.</li>
 *   <li><b>Saying it once per family instead of once per batch.</b> "Sign out everywhere" revokes thousands
 *       of rows in one statement, and one walk of the connected clients per family is the quadratic version
 *       of the same work.</li>
 *   <li><b>Letting the announcement break the thing it was announcing.</b> The callers are a logout that has
 *       ALREADY happened in the database, and the poll — the one loop that must keep running for any other
 *       instance's revocations to be seen here. Neither may fail because nobody could be told.</li>
 * </ul>
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class RevocationBroadcastCheck {

    static int pass = 0, fail = 0;

    static final long NOW = 1_800_000_000_000L;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** Records every announcement as its own batch, so "twice" and "in one batch" are both visible. */
    static class Recorder implements RevocationBroadcastRegistry.RevocationBroadcaster {
        final List<List<String>> batches = new ArrayList<>();

        @Override
        public void onFamiliesRevoked(java.util.Collection<String> familyIds) {
            batches.add(new ArrayList<>(familyIds));
        }

        List<String> flat() {
            List<String> all = new ArrayList<>();
            for (List<String> batch : batches) all.addAll(batch);
            return all;
        }
    }

    static List<SessionFamilyStore.Revocation> revocations(String... familyIds) {
        List<SessionFamilyStore.Revocation> list = new ArrayList<>();
        for (String familyId : familyIds)
            list.add(new SessionFamilyStore.Revocation(familyId, NOW));
        return list;
    }

    public static void main(String[] args) {
        System.out.println("a revocation this instance performs is announced:");
        RevokedFamilies.clear();
        Recorder recorder = new Recorder();
        RevocationBroadcastRegistry.register(recorder);
        RevokedFamilies.note("fam-1", NOW);
        check("the family is announced", recorder.flat().equals(List.of("fam-1")));

        System.out.println("but only the first time it is heard:");
        // The poll's safety window re-reads the last few minutes on a timer, so this is not a rare case —
        // it is what happens to every revocation, a few minutes after it happens.
        RevokedFamilies.note("fam-1", NOW);
        RevokedFamilies.note("fam-1", NOW + 1000);
        check("re-noting the same family announces nothing more", recorder.flat().equals(List.of("fam-1")));

        System.out.println("a batch is announced as a batch, and only its new members:");
        RevokedFamilies.clear();
        recorder = new Recorder();
        RevocationBroadcastRegistry.register(recorder);
        int added = RevokedFamilies.noteAll(revocations("a", "b", "c"));
        check("all three counted as new", added == 3);
        check("and announced in ONE batch, not three", recorder.batches.size() == 1);
        check("naming all three", recorder.batches.get(0).equals(List.of("a", "b", "c")));

        // The overlapping read: two already known, one genuinely new.
        int addedAgain = RevokedFamilies.noteAll(revocations("b", "c", "d"));
        check("only the new one counts", addedAgain == 1);
        check("and only the new one is announced", recorder.batches.size() == 2
              && recorder.batches.get(1).equals(List.of("d")));

        System.out.println("a page with nothing new in it says nothing at all:");
        int quiet = RevokedFamilies.noteAll(revocations("a", "b", "c", "d"));
        check("nothing counted", quiet == 0);
        // An empty batch would be a log line and a walk of every connected client, to tell nobody anything.
        check("and no empty batch announced", recorder.batches.size() == 2);

        System.out.println("an announcement that fails does not fail the revocation:");
        RevokedFamilies.clear();
        RevocationBroadcastRegistry.register(familyIds -> {
            throw new IllegalStateException("the push layer is having a bad day");
        });
        boolean noteSurvived = true;
        try {
            RevokedFamilies.note("fam-2", NOW);
        } catch (Throwable e) {
            noteSurvived = false;
        }
        // A logout that already happened in the database must not report failure to the user, and the poll
        // must not die — it is how this instance learns about every OTHER instance's revocations.
        check("note() survives a broadcaster that throws", noteSurvived);
        check("and the family is still revoked, which is the part that enforces", RevokedFamilies.isRevoked("fam-2"));

        boolean noteAllSurvived = true;
        try {
            RevokedFamilies.noteAll(revocations("fam-3", "fam-4"));
        } catch (Throwable e) {
            noteAllSurvived = false;
        }
        check("noteAll() survives it too", noteAllSurvived);
        check("and its families are revoked", RevokedFamilies.isRevoked("fam-3") && RevokedFamilies.isRevoked("fam-4"));

        System.out.println("with nothing registered, revocation is exactly as it was before:");
        RevokedFamilies.clear();
        RevocationBroadcastRegistry.register(null);
        boolean unregisteredSurvived = true;
        try {
            RevokedFamilies.note("fam-5", NOW);
            RevokedFamilies.noteAll(revocations("fam-6"));
        } catch (Throwable e) {
            unregisteredSurvived = false;
        }
        check("nothing throws", unregisteredSurvived);
        check("and the families are revoked", RevokedFamilies.isRevoked("fam-5") && RevokedFamilies.isRevoked("fam-6"));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
