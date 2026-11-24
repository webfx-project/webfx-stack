package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Sign out my other devices" — and the two rules that keep it from becoming "sign out anybody".
 *
 * <p>The feature is one statement against a table. What needs checking is not the statement but what
 * the server is willing to believe about who is asking:
 *
 * <ul>
 *   <li><b>It takes no target.</b> The person and the session to spare come from the state the syncer
 *       wrote after this call's token verified, never from anything the caller sent. A target
 *       parameter would be an unauthenticated way to end any member's sessions, and with sequential
 *       person ids, every member's.</li>
 *   <li><b>It requires a token, not a claim.</b> While the identity flip is off a client may still
 *       assert who it is with no token at all; acting on that assertion here would hand the same
 *       power back. The family id is set only from a verified token, so requiring one is the test.</li>
 * </ul>
 *
 * <p>Two more rules sit beside those. A SUPPORT VIEW may not use it at all: its principal carries the
 * viewed member's person id, so unchecked it would end that member's sessions and spare the agent's.
 * And the device asking keeps its own session — signing out the device you are asking from would make
 * the feature hostile to its own use — while the sessions it ends are refused on sight at once rather
 * than at their next renewal.
 *
 * <p>The guest rule — a principal with no person behind it has nothing to sign out — lives in the
 * store that knows what a person is, and is checked there: ModalityAuthSessionStoreCheck.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class RevokeOtherSessionsCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** Records who was asked about, and ends everything but the spared family. */
    static class FamilyStore implements SessionFamilyStore {
        final Map<String, Object> livePrincipalByFamily = new LinkedHashMap<>();
        Object askedAbout;
        String askedToSpare;
        String reasonGiven;

        void open(String familyId, Object principal) {
            livePrincipalByFamily.put(familyId, principal);
        }

        @Override
        public Future<List<String>> revokeOtherFamilies(Object principal, String exceptFamilyId, String reason) {
            askedAbout = principal;
            askedToSpare = exceptFamilyId;
            reasonGiven = reason;
            List<String> ended = new ArrayList<>();
            livePrincipalByFamily.forEach((familyId, owner) -> {
                if (owner.equals(principal) && !familyId.equals(exceptFamilyId))
                    ended.add(familyId);
            });
            ended.forEach(livePrincipalByFamily::remove);
            return Future.succeededFuture(ended);
        }

        @Override public Future<String> open(Object principal, SessionTier tier, long absoluteExpiryMillis) { return Future.failedFuture("unused"); }
        @Override public Future<FamilyRenewal> renew(String familyId, int presentedGeneration, long nowMillis) { return Future.failedFuture("unused"); }
        @Override public Future<Void> revoke(String familyId, String reason) { return Future.failedFuture("unused"); }
        @Override public Future<RevocationPage> revokedSince(long sinceMillis, RevocationCursor after) { return Future.succeededFuture(RevocationPage.EMPTY); }
    }

    /** The state the syncer leaves behind once a token has verified: a principal and its family. */
    static Object verifiedState(Object principal, String familyId) {
        Object state = StateAccessor.createUserIdState(principal);
        return familyId == null ? state : StateAccessor.setSessionFamilyId(state, familyId);
    }

    static Future<List<String>> revokeAs(Object state) {
        return ThreadLocalStateHolder.runWithState(state, SessionTokenService::revokeOtherSessionsOfCurrentUser);
    }

    /** The panic button's half: end every session of this person, the calling one included. */
    static Future<List<String>> endEverySessionAs(Object state) {
        return ThreadLocalStateHolder.runWithState(state, SessionTokenService::endEverySessionOfCurrentUser);
    }

    public static void main(String[] args) {
        CheckPrincipal me = new CheckPrincipal(42, 7);
        CheckPrincipal someoneElse = new CheckPrincipal(43, 8);
        // What a deployment has: the password gateway registers this predicate at boot, and it answers
        // "no" for an ordinary member. Without one registered the control refuses outright — asserted
        // below, and the reason the ordinary cases need it in place first.
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(principal -> false);

        System.out.println("the ordinary case — three of my devices, one of them asking:");
        RevokedFamilies.clear();
        FamilyStore store = new FamilyStore();
        SessionFamilyStoreRegistry.register(store);
        store.open("mine-here", me);
        store.open("mine-phone", me);
        store.open("mine-tablet", me);
        store.open("someone-else", someoneElse);
        List<String> ended = revokeAs(verifiedState(me, "mine-here")).result();
        check("both of my other sessions end", ended.size() == 2 && ended.contains("mine-phone") && ended.contains("mine-tablet"));
        check("the device asking keeps its own", store.livePrincipalByFamily.containsKey("mine-here"));
        check("somebody else's session is untouched", store.livePrincipalByFamily.containsKey("someone-else"));
        check("and the store was asked about ME, and told to spare THIS session",
              me.equals(store.askedAbout) && "mine-here".equals(store.askedToSpare));
        // Not "user", which is what a logout writes: the one column that could answer "was I signed
        // out, or did I sign myself out?" must not read the same either way.
        check("recorded distinctly from an ordinary logout",
              "signed-out-elsewhere".equals(store.reasonGiven));
        // Those devices stop at their next message rather than at their next renewal, which is the
        // whole value of having revoked them promptly.
        check("the ended sessions are refused on sight at once",
              RevokedFamilies.isRevoked("mine-phone") && RevokedFamilies.isRevoked("mine-tablet"));
        check("and the surviving one is not", !RevokedFamilies.isRevoked("mine-here"));

        System.out.println("a caller with no verified session cannot use it at all:");
        RevokedFamilies.clear();
        FamilyStore guarded = new FamilyStore();
        SessionFamilyStoreRegistry.register(guarded);
        guarded.open("victim-session", someoneElse);
        // Exactly what a client can still send while the flip is off: a principal it simply asserts,
        // with no token behind it — so no family id was ever written into the state.
        check("a claimed identity with no token is refused", revokeAs(verifiedState(someoneElse, null)).failed());
        check("and nothing of theirs was touched", guarded.livePrincipalByFamily.containsKey("victim-session"));
        check("the store was never even asked", guarded.askedAbout == null);

        System.out.println("there is no way to name somebody else:");
        // Pinned at the signature, because this is the property that would be lost by a helpful edit —
        // someone adding a personId "so an admin can do it too" turns a self-service control into an
        // unauthenticated way to end any member's sessions. If that parameter ever appears, this fails
        // before anyone has to reason about who may call it.
        boolean takesNoTarget;
        try {
            takesNoTarget = SessionTokenService.class.getMethod("revokeOtherSessionsOfCurrentUser")
                                                     .getParameterCount() == 0;
        } catch (NoSuchMethodException e) {
            takesNoTarget = false;
        }
        check("the call takes no argument at all, so there is nobody to name", takesNoTarget);

        System.out.println("a support view may not sign the member out of their own devices:");
        RevokedFamilies.clear();
        FamilyStore viewed = new FamilyStore();
        SessionFamilyStoreRegistry.register(viewed);
        viewed.open("members-phone", me);
        viewed.open("agents-support-view", me);
        // A support view's principal carries the MEMBER's person id - that is how it reads their data -
        // so without a check it ends the member's sessions and spares the agent's. The registry is what
        // says which principals are restricted; here, this one is.
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(principal -> me.equals(principal));
        Future<List<String>> refused = revokeAs(verifiedState(me, "agents-support-view"));
        check("it is refused", refused.failed());
        check("and the member keeps every session they had",
              viewed.livePrincipalByFamily.containsKey("members-phone"));
        // Fails closed, unlike the global write gate: an unanswerable question here refuses one control
        // rather than bricking every write in the application.
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(null);
        check("and an unanswerable question refuses too",
              revokeAs(verifiedState(me, "agents-support-view")).failed());
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(principal -> false);

        System.out.println("the panic button — my device has been stolen:");
        RevokedFamilies.clear();
        FamilyStore stolen = new FamilyStore();
        SessionFamilyStoreRegistry.register(stolen);
        stolen.open("the-stolen-laptop", me);
        stolen.open("mine-phone", me);
        stolen.open("mine-here", me);
        stolen.open("someone-else", someoneElse);
        List<String> all = endEverySessionAs(verifiedState(me, "mine-here")).result();
        // Signing yourself out too is the point, not a side effect: this call cannot tell which family is
        // the thief's, and stopping them matters more than keeping the owner working.
        check("every session of mine ends, the one asking included", all.size() == 3 && all.contains("mine-here"));
        check("nothing of mine is left open", stolen.livePrincipalByFamily.keySet().stream().noneMatch(f -> me.equals(stolen.livePrincipalByFamily.get(f))));
        check("somebody else's session is still untouched", stolen.livePrincipalByFamily.containsKey("someone-else"));
        // "" and not the caller's family: `id <> ''` holds for every real family, which is what takes the
        // caller's own with the rest.
        check("the store was told to spare nothing", "".equals(stolen.askedToSpare));
        // A rescue an hour later reads these rows to tell this from an ordinary sign-out.
        check("recorded as a theft, not as signing out elsewhere", "device-stolen".equals(stolen.reasonGiven));

        System.out.println("the panic button refuses what the other control refuses:");
        RevokedFamilies.clear();
        FamilyStore panicViewed = new FamilyStore();
        SessionFamilyStoreRegistry.register(panicViewed);
        panicViewed.open("members-phone", me);
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(principal -> me.equals(principal));
        check("a support view may not end the member's sessions either",
              endEverySessionAs(verifiedState(me, "agents-support-view")).failed());
        check("and the member keeps every session they had",
              panicViewed.livePrincipalByFamily.containsKey("members-phone"));
        RestrictedPrincipalRegistry.registerRestrictedUserPredicate(principal -> false);
        check("a session with no verified family is refused, as it is for signing out others",
              endEverySessionAs(verifiedState(me, null)).failed());

        System.out.println("a deployment that records no sessions:");
        RevokedFamilies.clear();
        SessionFamilyStoreRegistry.register(null);
        Future<List<String>> noStore = revokeAs(verifiedState(me, "mine-here"));
        check("succeeds with nothing to end, rather than failing", noStore.succeeded() && noStore.result().isEmpty());

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
