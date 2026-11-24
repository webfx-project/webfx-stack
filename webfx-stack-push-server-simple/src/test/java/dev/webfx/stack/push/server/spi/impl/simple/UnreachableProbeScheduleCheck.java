package dev.webfx.stack.push.server.spi.impl.simple;

/**
 * How long an unreachable push client is given before it is dropped.
 *
 * <p>Dropping a client drops every query-push stream it holds, server-side, and the client is never told:
 * its socket stays up, its status reads "Connected", and its live lists stop changing. That is what one
 * failed push used to cost (2026-10-10). The schedule below is what replaced it, so the properties that
 * matter are pinned here: a client is never given up on the first failure, a client reconnecting within
 * seconds is caught by an early probe, and the client IS given up eventually — a schedule that never ends
 * would keep a gone client's streams running their queries for ever.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class UnreachableProbeScheduleCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        long[] delays = SimplePushServerServiceProvider.UNREACHABLE_PROBE_DELAYS_MS;

        System.out.println("a failed push is not the end of the client:");
        check("there is at least one probe before giving up", SimplePushServerServiceProvider.nextProbeDelay(0) > 0);

        System.out.println("a client back within seconds is noticed within seconds:");
        // A replaced socket re-registers its push address within the client's 2s reconnection delay.
        check("the first probe comes within 5s", SimplePushServerServiceProvider.nextProbeDelay(0) <= 5_000);

        System.out.println("probes back off and end:");
        boolean nonDecreasing = true;
        long total = 0;
        for (int i = 0; i < delays.length; i++) {
            if (i > 0 && delays[i] < delays[i - 1])
                nonDecreasing = false;
            total += SimplePushServerServiceProvider.nextProbeDelay(i);
        }
        check("each probe waits at least as long as the one before", nonDecreasing);
        // Long enough to ride out a reconnection with backoff; short enough that a gone client's streams
        // stop costing queries soon. The client's own silence watchdog (90s) covers what lies beyond.
        check("the client gets between one and five minutes in all (" + total + "ms)", total >= 60_000 && total <= 300_000);
        check("after the last probe the client is given up", SimplePushServerServiceProvider.nextProbeDelay(delays.length) == -1);
        check("and stays given up", SimplePushServerServiceProvider.nextProbeDelay(delays.length + 5) == -1);

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }
}
