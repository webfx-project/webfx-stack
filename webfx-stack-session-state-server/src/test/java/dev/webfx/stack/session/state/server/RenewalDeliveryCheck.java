package dev.webfx.stack.session.state.server;

import dev.webfx.stack.session.state.LogoutUserId;

/**
 * When a renewed identity token may be handed to a client, and — the part that matters — when it may not.
 *
 * <p>These are not edge cases. A renewal mints a live proof of an identity and holds it until the client
 * picks it up, and the outgoing path staples it to messages the client did not ask for. Two of those
 * messages must never carry it, and both bugs were real before these checks existed:
 *
 * <ul>
 *   <li><b>A logout.</b> Stapling a working token to "you are logged out" hands the client a proof of the
 *       identity it was just told to forget. On a shared device the next person reloads and is signed in
 *       as the member who thought they had signed out — a logout that does not log out.</li>
 *   <li><b>A login.</b> That message already carries the token a gateway minted after checking a
 *       credential, and setUserToken overrides — so a renewal pending for the PREVIOUS occupant of the
 *       session would replace it, and someone who had just typed their own password would be handed the
 *       account of whoever used the device before them.</li>
 * </ul>
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core: this repository declares no
 * JUnit. Run from main(); it exits non-zero while the issue stands.
 */
public class RenewalDeliveryCheck {

    static int pass = 0, fail = 0;
    static final Object USER = "some-principal";

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("the ordinary case — a reply that says nothing about identity:");
        check("a signed-in session gets its renewed token",
              ServerSideStateSessionSyncer.mayDeliverPendingToken(null, null, USER));

        System.out.println("a logout must not carry a live token:");
        check("the logout message itself is refused",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(LogoutUserId.LOGOUT_USER_ID, null, USER));
        check("and so is everything after it, once the session holds the logout",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(null, null, LogoutUserId.LOGOUT_USER_ID));
        check("including a session that never had an identity",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(null, null, null));

        System.out.println("a login must keep the token its own credential check minted:");
        check("a message carrying a token is left alone",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(USER, "freshly-minted", USER));
        check("and so is one that names a user without a token",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(USER, null, USER));
        check("a token with no user id beside it is still a token",
              !ServerSideStateSessionSyncer.mayDeliverPendingToken(null, "freshly-minted", USER));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
