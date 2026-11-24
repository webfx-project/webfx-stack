package dev.webfx.stack.session.state.server;

import dev.webfx.stack.session.token.SecurityAlarm;

/**
 * How often the identity-path counters report, which is the whole of control 4's "raise logging".
 *
 * <p>The thing worth pinning is not the numbers but the SHAPE of the change: an alarm moves the cadence
 * and nothing else. Both figures those counters report are aggregates written that way on purpose — a
 * client echoes its token on every message, so per-message logging would report one stale tab as hundreds
 * a minute and bury the very signal an operator is reading for. It would be an easy and plausible "fix" to
 * make an alarm log every occurrence; this check exists so that doing so fails here first.
 *
 * <p>It also pins the direction. A cadence that grew longer under an alarm would be worse than no change
 * at all, since the alarm is exactly the half-hour in which somebody is watching the log.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core: this repository declares no
 * JUnit. Run from main(); it exits non-zero while the issue stands.
 */
public class IdentityLogCadenceCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ✓ " + what); }
        else { fail++; System.out.println("  ✗ " + what); }
    }

    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        System.out.println("with no alarm, the ordinary cadence:");
        SecurityAlarm.clear();
        long quiet = ServerSideStateSessionSyncer.identityLogIntervalMillis(now);
        check("reports once a minute", quiet == 60_000);

        System.out.println("while an alarm is raised:");
        SecurityAlarm.noteExpiry(now + SecurityAlarm.RAISE_DURATION_MILLIS);
        long raised = ServerSideStateSessionSyncer.identityLogIntervalMillis(now);
        check("reports far more often", raised < quiet);
        check("but still aggregates rather than logging every message — a cadence, not a firehose",
              raised > 0);
        check("matched to the alarm poll, so it cannot report a staler posture than it polls",
              raised == 5_000);

        System.out.println("when the alarm lapses, the cadence goes back by itself:");
        check("an expiry in the past is not raised",
              ServerSideStateSessionSyncer.identityLogIntervalMillis(now + SecurityAlarm.RAISE_DURATION_MILLIS + 1) == quiet);
        SecurityAlarm.clear();
        check("and a cleared alarm is not raised either",
              ServerSideStateSessionSyncer.identityLogIntervalMillis(now) == quiet);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
