package dev.webfx.stack.authn.spi.impl.server.portal;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.token.SessionFamilyStore;
import dev.webfx.stack.session.token.SessionFamilyStoreRegistry;
import dev.webfx.stack.session.token.SessionTier;

import java.util.List;

/**
 * Logout reaches the gateway AS THE CALLER, even though it ends the session family first.
 *
 * <p>Ending the family is a database round trip, and the thread-local state that says who is calling is
 * restored the moment the synchronous part of logout() returns. A gateway consulted after that hop sees
 * nobody: it refuses, the device that asked is never told, and its server session goes on naming the
 * user. That shipped — every logout ended the family and then failed with "no server gateway accepted
 * UserId null" — and it looked like a client bug, because the page that had just signed out was signed
 * straight back in by a renewed token stapled to its next push.
 *
 * <p>The store here completes its revoke LATER, once the caller's state has left the thread, because that
 * is what a database does. A store answering with an already-completed future would run the gateway inside
 * the caller's state and pass this check with the bug still in place.
 *
 * <p>The gateway is handed over directly rather than registered through META-INF/services: ServiceLoader
 * ignores that file for a class patched into a named module, so the check would pass or fail depending on
 * how it was launched.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class LogoutCheck {

    static int pass = 0, fail = 0;

    static final Object USER = "some-principal";
    static final String RUN_ID = "run-of-the-device-logging-out";
    static final String FAMILY = "family-of-that-session";

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** Records what the portal lets a gateway see. */
    static class RecordingGateway implements ServerAuthenticationGateway {
        Object userIdSeenByAccepts;
        String runIdSeenByLogout;
        boolean loggedOut;

        @Override public boolean acceptsUserId() {
            userIdSeenByAccepts = ThreadLocalStateHolder.getUserId();
            return USER.equals(userIdSeenByAccepts);
        }

        @Override public Future<Void> logout() {
            loggedOut = true;
            runIdSeenByLogout = ThreadLocalStateHolder.getRunId(); // what LogoutPush addresses its push to
            return Future.succeededFuture();
        }

        boolean loggedOutAsTheCaller() {
            return loggedOut && USER.equals(userIdSeenByAccepts) && RUN_ID.equals(runIdSeenByLogout);
        }

        @Override public boolean acceptsUserCredentials(Object userCredentials) { return false; }
        @Override public Future<?> authenticate(Object userCredentials) { return Future.failedFuture("unused"); }
        @Override public Future<?> verifyAuthenticated() { return Future.failedFuture("unused"); }
        @Override public Future<UserClaims> getUserClaims() { return Future.failedFuture("unused"); }
        @Override public boolean acceptsUpdateCredentialsArgument(Object argument) { return false; }
        @Override public Future<?> updateCredentials(Object argument) { return Future.failedFuture("unused"); }
    }

    /** Ends a family when told to, not when asked — the gap a database round trip leaves. */
    static class SlowStore implements SessionFamilyStore {
        final Promise<Void> revoked = Promise.promise();
        String revokedFamily;

        @Override public Future<Void> revoke(String familyId, String reason) {
            revokedFamily = familyId;
            return revoked.future();
        }

        @Override public Future<String> open(Object principal, SessionTier tier, long absoluteExpiryMillis) { return Future.failedFuture("unused"); }
        @Override public Future<FamilyRenewal> renew(String familyId, int presentedGeneration, long nowMillis) { return Future.failedFuture("unused"); }
    }

    /** Throws instead of answering — a service not ready yet, an interceptor refusing on the spot. */
    static class ThrowingStore extends SlowStore {
        @Override public Future<Void> revoke(String familyId, String reason) {
            throw new IllegalStateException("store not ready");
        }
    }

    static Object callerState(String familyId) {
        Object state = StateAccessor.setRunId(StateAccessor.createUserIdState(USER), RUN_ID);
        return familyId == null ? state : StateAccessor.setSessionFamilyId(state, familyId);
    }

    static Future<Void> logOutAs(Object callerState, RecordingGateway gateway) {
        return ThreadLocalStateHolder.runWithState(callerState,
            () -> ServerAuthenticationPortalProvider.logoutWith(List.of(gateway)));
    }

    public static void main(String[] args) {
        System.out.println("a session with a family — the family is ended first, across a round trip:");
        SlowStore store = new SlowStore();
        SessionFamilyStoreRegistry.register(store);
        RecordingGateway gateway = new RecordingGateway();
        Future<Void> logout = logOutAs(callerState(FAMILY), gateway);
        check("the family being ended is the caller's", FAMILY.equals(store.revokedFamily));
        check("the device is not logged out before the family has ended", !gateway.loggedOut);
        check("and the caller's state has already left the thread", ThreadLocalStateHolder.getThreadLocalState() == null);

        store.revoked.complete(); // the database answers, on a thread that no longer knows who asked
        check("the gateway is asked about the CALLER, not about nobody", USER.equals(gateway.userIdSeenByAccepts));
        check("so the device is logged out", gateway.loggedOut);
        check("and told so on its own connection", RUN_ID.equals(gateway.runIdSeenByLogout));
        check("and the logout succeeds", logout.succeeded());
        check("without leaving the caller's state on the thread", ThreadLocalStateHolder.getThreadLocalState() == null);

        System.out.println("a store that fails, asynchronously or on the spot, still lets the device log out:");
        SlowStore failingStore = new SlowStore();
        SessionFamilyStoreRegistry.register(failingStore);
        gateway = new RecordingGateway();
        logout = logOutAs(callerState(FAMILY), gateway);
        failingStore.revoked.fail("database unavailable");
        check("a failed revoke is reported, not passed on", gateway.loggedOutAsTheCaller() && logout.succeeded());
        SessionFamilyStoreRegistry.register(new ThrowingStore());
        gateway = new RecordingGateway();
        try {
            logout = logOutAs(callerState(FAMILY), gateway);
            check("a store that throws does not escape the logout", true);
            check("and the device is still logged out, as the caller", gateway.loggedOutAsTheCaller() && logout.succeeded());
        } catch (RuntimeException e) {
            check("a store that throws does not escape the logout (threw " + e + ")", false);
        }

        System.out.println("a session with no family — a legacy token, or none — takes no round trip at all:");
        SessionFamilyStoreRegistry.register(new SlowStore());
        gateway = new RecordingGateway();
        logout = logOutAs(callerState(null), gateway);
        check("the device is logged out at once, as the caller", gateway.loggedOutAsTheCaller());
        check("and the logout succeeds", logout.succeeded());

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
