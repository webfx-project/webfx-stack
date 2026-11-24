package dev.webfx.stack.push.server.spi.impl.simple;

/**
 * Asking which app a THIRD PARTY's client is, without that question having side effects.
 *
 * <p>One caller needs this: the magic-link requester push, which signs in the tab that asked for a link
 * while running inside the request of the tab that clicked it. Every other mint site reads the flag from
 * its own connection at method entry; this one cannot, because the thread belongs to the other client.
 *
 * <p>Two properties, and both are load-bearing for different reasons.
 *
 * <p><b>Unknown must stay unknown.</b> The answer decides an audience that is then signed into a token
 * and cannot be corrected by anything the client says later. A caller that reads null as "front office"
 * hands somebody a session stamped for an app they are not — so null is returned for a runId nobody has
 * registered AND for a registered client that has not reported its app, because those are the same
 * answer. The caller's job is to decline, not to guess; this one's is not to pretend.
 *
 * <p><b>Asking must not create.</b> The provider's own {@code push()} path uses getOrCreate, and reaching
 * for it here would mean that asking about a client brings one into existence: the monitor's client count
 * would climb with every magic link redeemed, and a dead connection would reappear as a live one. The
 * check below counts clients before and after, because the mistake is a one-word edit away and would
 * never show up as a failure anywhere else.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class ClientBackofficeLookupCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        SimplePushServerServiceProvider provider = new SimplePushServerServiceProvider();

        System.out.println("a client nobody has registered answers \"do not know\":");
        check("an unknown runId is null, not FALSE", provider.getClientBackoffice("run-nobody") == null);
        check("a null runId is null rather than an error", provider.getClientBackoffice(null) == null);

        System.out.println("...and asking did not bring one into existence:");
        // The one-word mistake: getOrCreate instead of get. It would inflate the monitor's count on every
        // magic link redeemed, and resurrect connections that had gone.
        check("no client was created by asking about it", provider.getPushClientsCount() == 0);

        System.out.println("a registered client that has not said which app it is also answers \"do not know\":");
        // setClientMetadata attaches to an already-registered client and never creates one, and
        // clientIsLive is get-only too, so registration goes through the seam. Then supply everything
        // the live tick supplies EXCEPT the app.
        provider.getOrCreatePushClientInfo("run-quiet");
        provider.setClientMetadata("run-quiet", null, "1.2.3", Boolean.FALSE, "chrome|mac|desktop", null);
        check("null, the same answer as never having been seen", provider.getClientBackoffice("run-quiet") == null);
        check("even though the client itself is registered", provider.getPushClientsCount() == 1);

        System.out.println("once a client reports its app, that is what comes back:");
        provider.getOrCreatePushClientInfo("run-bo");
        provider.setClientMetadata("run-bo", null, "1.2.3", Boolean.FALSE, "chrome|mac|desktop", Boolean.TRUE);
        provider.getOrCreatePushClientInfo("run-fo");
        provider.setClientMetadata("run-fo", null, "1.2.3", Boolean.FALSE, "safari|ios|phone", Boolean.FALSE);
        check("the back-office client answers TRUE", Boolean.TRUE.equals(provider.getClientBackoffice("run-bo")));
        check("the front-office client answers FALSE", Boolean.FALSE.equals(provider.getClientBackoffice("run-fo")));
        check("and they are not confused with one another",
              !provider.getClientBackoffice("run-bo").equals(provider.getClientBackoffice("run-fo")));

        System.out.println("the answer is per client, so one quiet client does not borrow another's:");
        // The failure this would be: the requester push reading the REDEEMER's app because the lookup fell
        // back to "any client" or to a last-written value. That is the bug being fixed, reintroduced one
        // layer down, and it would look like the fix working for anyone testing in a single browser.
        check("the quiet one still knows nothing, with two apps registered beside it",
              provider.getClientBackoffice("run-quiet") == null);
        check("and an unknown runId still knows nothing", provider.getClientBackoffice("run-nobody") == null);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
