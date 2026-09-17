package dev.webfx.stack.webpush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.webpush.WebPushPayload;
import dev.webfx.stack.webpush.WebPushResult;
import dev.webfx.stack.webpush.WebPushServerService;
import dev.webfx.stack.webpush.WebPushSubscription;
import dev.webfx.stack.webpush.spi.WebPushSubscriptionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Server-side executor for the {@code SendPushNotification} BO operation.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Reject if the caller is not authenticated.</li>
 *   <li>Look up the caller's email — needed for {@code testSendToSelf} and
 *       useful for the log line either way.</li>
 *   <li>Delegate to {@link WebPushSubscriptionStore#findRecipients} with the
 *       opaque target object and the email filter; the store impl translates
 *       the target into a DB query against its own entity model.</li>
 *   <li>Loop, calling {@link WebPushServerService#send} for each subscription,
 *       and aggregate the result variants into the response.</li>
 * </ol>
 *
 * <h2>Authorization</h2>
 * <p>V1 only requires authentication; the per-role gate is enforced on the
 * client by hiding the BO page behind {@code hasPermission("SendPushNotification")}.
 * A proper server-side role check is the V2 follow-up — needs the host's
 * authorization layer to define the corresponding Operation.
 *
 * @author Bruno Salmon
 */
final class SendPushNotificationExecutor {

    private SendPushNotificationExecutor() {}

    static Future<SendPushNotificationResult> execute(SendPushNotificationArgument arg) {
        if (ThreadLocalStateHolder.getUserId() == null) {
            return Future.failedFuture("Not authenticated");
        }
        // A read-only session (a support member borrowing another user's view) must not be able to
        // broadcast notifications to real devices. Sending never touches the SQL submit layer where
        // read-only is otherwise enforced, so this endpoint has to refuse for itself — checked here,
        // synchronously, while the thread-local still holds the principal.
        if (RestrictedPrincipalRegistry.isCurrentUserRestricted()) {
            return Future.failedFuture("[ReadOnlySessionError] This session is not allowed to send notifications");
        }
        return AuthenticationService.getUserClaims()
                .compose(claims -> runForCaller(arg, claims == null ? null : claims.email()));
    }

    private static Future<SendPushNotificationResult> runForCaller(
            SendPushNotificationArgument arg, String callerEmail) {
        String emailFilter = null;
        if (arg.testSendToSelf()) {
            if (callerEmail == null || callerEmail.isBlank()) {
                // Test-send requires the caller's email — without it we'd
                // accidentally broadcast. Fail loudly rather than silently
                // sending nothing.
                return Future.failedFuture("testSendToSelf=true but caller has no email");
            }
            emailFilter = callerEmail;
        }
        WebPushSubscriptionStore store = getStore();
        if (store == null) {
            return Future.failedFuture("No WebPushSubscriptionStore registered");
        }
        final String finalEmailFilter = emailFilter;
        return store.findRecipients(arg.target(), emailFilter)
                .compose(subs -> sendAll(filterByCurrentVapidKey(subs, callerEmail),
                                         arg, callerEmail, finalEmailFilter));
    }

    /**
     * Drop subscriptions whose stored {@code vapidPublicKey} doesn't match
     * the server's current VAPID identity. Catches two scenarios:
     * <ul>
     *   <li><b>Env crossover</b>: after a prod→staging DB copy, staging holds
     *       production-origin subscriptions. Sending to them would have the
     *       push service return 403 (VAPID JWT signed with staging's key but
     *       the subscription expects production's). Skipping them avoids the
     *       round-trip and keeps the failed-count meaningful.</li>
     *   <li><b>Key rotation</b>: if the server's VAPID keypair is rotated,
     *       all pre-rotation subscriptions become unreachable. Same treatment.</li>
     * </ul>
     * Subscriptions with {@code vapidPublicKey == null} (legacy rows that
     * pre-date the column) pass through — once backfilled, all rows will have
     * a non-null value and this lenient branch becomes a no-op.
     */
    private static List<WebPushSubscription> filterByCurrentVapidKey(
            List<WebPushSubscription> subs, String callerEmail) {
        String currentKey = WebPushServerService.currentVapidPublicKey();
        if (currentKey == null) {
            // No VAPID config — Web Push is disabled anyway; we wouldn't get
            // here in practice since the endpoint registration is also gated.
            return subs;
        }
        int dropped = 0;
        List<WebPushSubscription> kept = new ArrayList<>(subs.size());
        for (WebPushSubscription s : subs) {
            if (s.vapidPublicKey() == null || currentKey.equals(s.vapidPublicKey())) {
                kept.add(s);
            } else {
                dropped++;
            }
        }
        if (dropped > 0) {
            Console.log("[SendPushNotification] caller=" + callerEmail
                    + " — skipped " + dropped + " subscription(s) with foreign VAPID key"
                    + " (likely env crossover or pre-rotation orphan)");
        }
        return kept;
    }

    private static Future<SendPushNotificationResult> sendAll(
            List<WebPushSubscription> subs,
            SendPushNotificationArgument arg,
            String callerEmail,
            String emailFilter) {
        int targeted = subs.size();
        if (targeted == 0) {
            Console.log("[SendPushNotification] caller=" + callerEmail
                    + " target=" + arg.target()
                    + " test=" + (emailFilter != null) + " — no recipients");
            return Future.succeededFuture(new SendPushNotificationResult(0, 0, 0, 0));
        }

        WebPushPayload payload = new WebPushPayload(
                arg.title(), arg.body(), arg.url(),
                // tag = stable per-target so multiple sends to the same audience
                // collapse on the device rather than stacking.
                "push-" + System.identityHashCode(arg.target()),
                /*ttlSeconds=*/86400,
                WebPushPayload.Urgency.NORMAL);

        List<Future<WebPushResult>> sends = new ArrayList<>(targeted);
        for (WebPushSubscription sub : subs) {
            sends.add(WebPushServerService.send(sub, payload));
        }

        return Future.all(sends).map(cf -> {
            int succeeded = 0, expired = 0, failed = 0;
            for (int i = 0; i < sends.size(); i++) {
                WebPushResult r = cf.resultAt(i);
                if (r instanceof WebPushResult.Success)              succeeded++;
                else if (r instanceof WebPushResult.SubscriptionExpired) expired++;
                else                                                  failed++;
            }
            SendPushNotificationResult result = new SendPushNotificationResult(
                    targeted, succeeded, expired, failed);
            Console.log("[SendPushNotification] caller=" + callerEmail
                    + " target=" + arg.target()
                    + " test=" + (emailFilter != null)
                    + " → " + result);
            return result;
        });
    }

    private static WebPushSubscriptionStore getStore() {
        // SPI class extracted to a local — referencing it twice inline causes
        // webfx-cli to emit duplicate `uses` directives in module-info.java.
        Class<WebPushSubscriptionStore> spi = WebPushSubscriptionStore.class;
        return SingleServiceProvider.getProvider(spi, () -> ServiceLoader.load(spi),
                SingleServiceProvider.NotFoundPolicy.RETURN_NULL);
    }
}
