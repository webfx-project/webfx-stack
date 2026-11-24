package dev.webfx.stack.push.server.spi.impl.simple;

/**
 * Who may say which session family a connected client belongs to.
 *
 * <p>This is the access control on prompt revocation, and it exists because the two halves of that
 * statement come from sources with very different trust. The <b>family</b> is taken from a token whose
 * signature this server verified, so nobody can name a family that is not theirs. The <b>runId</b> naming
 * the client arrives in an ordinary message and is authenticated by nothing at all.
 *
 * <p>Believe the pair and the attack writes itself: log in normally, send one message carrying your own
 * genuine token and somebody else's runId, and their client is now filed under your family — then end your
 * own session and theirs ends with it. One message per victim, and the only thing standing in the way is
 * that runIds are not published, which is not a security property anybody promised.
 *
 * <p>So: the first session to claim a runId owns it. The rule below is stated separately from the code
 * that applies it because the obvious future "improvement" — letting a client that lost its session take
 * its runId back — is exactly the hole, and it should be hard to make by accident.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class FamilyClaimCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("an unclaimed client may be claimed:");
        check("nobody owns it yet, so the first session to say so does",
              SimplePushServerServiceProvider.isFamilyClaimAllowed(null, "session-a"));

        System.out.println("its owner may keep speaking for it:");
        // The ordinary case, on every live tick for the life of the connection — and the case that has to
        // keep working through a login, a logout and a token rotation, all of which change the family.
        check("the same session is still allowed", SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", "session-a"));

        System.out.println("nobody else may:");
        // The attack: an attacker's session, holding its own perfectly valid token, naming a runId that
        // belongs to somebody else's browser.
        check("a different session is refused", !SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", "session-b"));
        check("and so is an absent one, which is a claim proving nothing",
              !SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", null));

        System.out.println("the rule is about the session, not about the family it wants to set:");
        // Worth stating because the refusal has to hold even when the claim looks innocent. An attacker
        // ending their own session is an ordinary logout; the damage is done by whose client it reaches.
        check("an owner is decided once and does not drift",
              SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", "session-a")
              && !SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", "session-b")
              && SimplePushServerServiceProvider.isFamilyClaimAllowed("session-a", "session-a"));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
