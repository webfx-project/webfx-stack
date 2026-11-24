package dev.webfx.stack.session.token;

/**
 * What alarm mode does to the access window, and — the part that is easy to get wrong — what it does to
 * the sessions that already exist when it is raised.
 *
 * <p>Their expiry was signed when they were minted and cannot be rewritten, so a shortened window can only
 * reach them through the renewal rule: a token with more life left than the alarm allows is due at once.
 * The mirror of that is the case this check exists for — a token minted DURING the alarm has exactly the
 * alarm's window, and must not be caught by the same rule, or every message would renew.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero on failure.
 *
 * @author Claude Code
 */
public class SecurityAlarmCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** What the OTHER instance — the one holding the alarm — makes of the same token. */
    static boolean isRenewalDueWithAlarm(long nowMillis, long accessExpiryMillis) {
        SecurityAlarm.noteExpiry(nowMillis + 30 * 60 * 1000L);
        try {
            return SessionLifetime.isRenewalDue(nowMillis, accessExpiryMillis);
        } finally {
            SecurityAlarm.clear();
        }
    }

    public static void main(String[] args) {
        long now = 1_700_000_000_000L;
        long ordinaryWindow = SessionLifetime.ACCESS_WINDOW_BASE_MILLIS;
        long alarmWindow = SecurityAlarm.ACCESS_WINDOW_MILLIS;

        System.out.println("with no alarm raised:");
        SecurityAlarm.clear();
        check("the access window is the ordinary one", SessionLifetime.accessWindowMillis(now) == ordinaryWindow);
        check("a fresh token is not due for renewal",
              !SessionLifetime.isRenewalDue(now, now + ordinaryWindow));
        check("a token near the end of its window is", SessionLifetime.isRenewalDue(now, now + 1000));
        check("and the status reads as no alarm", SecurityAlarm.expiryMillis(now) == 0);

        System.out.println("while an alarm is raised:");
        SecurityAlarm.noteExpiry(now + 30 * 60 * 1000L);
        check("it is raised", SecurityAlarm.isRaised(now));
        check("the access window is the alarm's", SessionLifetime.accessWindowMillis(now) == alarmWindow);
        // The sessions that already existed: signed for half an hour, and that cannot be rewritten
        check("a session minted before the alarm, with 25 minutes left, renews AT ONCE",
              SessionLifetime.isRenewalDue(now, now + 25 * 60 * 1000L));
        check("so does one with just over the alarm window left",
              SessionLifetime.isRenewalDue(now, now + alarmWindow + 1000));
        // And the mirror: what it minted itself must not be caught by that same rule
        check("a token minted DURING the alarm is not immediately due",
              !SessionLifetime.isRenewalDue(now, SessionLifetime.accessExpiryFrom(now)));
        check("it becomes due near the end of its own short window",
              SessionLifetime.isRenewalDue(now + alarmWindow - 1000, now + alarmWindow));
        check("a token minted now gets the short window",
              SessionLifetime.accessExpiryFrom(now) == now + alarmWindow);

        System.out.println("an alarm ends by itself:");
        SecurityAlarm.noteExpiry(now + 1000);
        check("raised a second before it lapses", SecurityAlarm.isRaised(now));
        check("and not a second after", !SecurityAlarm.isRaised(now + 2000));
        check("the window is back to normal once it has lapsed",
              SessionLifetime.accessWindowMillis(now + 2000) == ordinaryWindow);
        check("a lapsed alarm reads as none rather than as a past moment",
              SecurityAlarm.expiryMillis(now + 2000) == 0);
        check("and the sessions it hurried are left alone again",
              !SessionLifetime.isRenewalDue(now + 2000, now + 2000 + 25 * 60 * 1000L));

        System.out.println("the store is the one place the answer lives:");
        SecurityAlarm.noteExpiry(now + 30 * 60 * 1000L);
        SecurityAlarm.noteExpiry(0); // a stand-down, or a row somebody removed
        check("an expiry that goes backwards is taken, so an alarm can be ended", !SecurityAlarm.isRaised(now));
        SecurityAlarm.noteExpiry(-1); // nothing sensible, from a store answering oddly
        check("nonsense reads as no alarm rather than as one", !SecurityAlarm.isRaised(now));

        System.out.println("the known edge — an instance that has not read the alarm yet:");
        SecurityAlarm.clear();
        // It sees a token another instance minted under the alarm, and judges it by its own window: two
        // minutes left against thirty is near-expiry, so it renews it long again. That is the disagreement
        // the poll interval bounds (see SecurityAlarm); pinned here so that it is a known property rather
        // than a surprise, and so a future fix has something to change deliberately.
        check("it renews an alarm-length token, because two minutes IS near-expiry against its own window",
              SessionLifetime.isRenewalDue(now, now + alarmWindow));
        check("and the alarmed instance hurries the long token that comes back",
              isRenewalDueWithAlarm(now, now + ordinaryWindow));

        System.out.println("it only ever SHORTENS the window:");
        SecurityAlarm.noteExpiry(now + 30 * 60 * 1000L);
        double previousScale = SessionLifetime.getScale();
        try {
            // A development scale already shorter than the alarm's window: the alarm must not lengthen it
            SessionLifetime.setScale(SessionLifetime.MINIMUM_SCALE);
            long scaledWindow = Math.round(SessionLifetime.ACCESS_WINDOW_BASE_MILLIS * SessionLifetime.MINIMUM_SCALE);
            check("a development scale shorter than the alarm's window keeps its own, shorter window",
                  scaledWindow < alarmWindow && SessionLifetime.accessWindowMillis(now) == scaledWindow);
        } finally {
            SessionLifetime.setScale(previousScale);
            SecurityAlarm.clear();
        }

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
