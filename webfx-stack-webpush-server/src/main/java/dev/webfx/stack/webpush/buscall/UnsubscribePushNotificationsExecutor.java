package dev.webfx.stack.webpush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.webpush.spi.WebPushSubscriptionStore;

import java.util.ServiceLoader;

/**
 * Server-side executor for the {@code UnsubscribePushNotifications} FO
 * operation.
 *
 * <h2>Auth resolution</h2>
 * <ol>
 *   <li><b>Authenticated</b>: the caller's auth-claim email is used, and the
 *       email field of the argument is ignored. Prevents a logged-in user from
 *       unsubscribing arbitrary addresses by tweaking the request body.</li>
 *   <li><b>Anonymous</b>: the email from the argument is used. Necessary for
 *       operator-shared {@code /unsubscribe?email=…} links to work without
 *       requiring the user to remember their password just to opt out.</li>
 * </ol>
 *
 * <p>If neither resolution yields an email, the operation fails — the FO page
 * should never let the user submit an empty form, but defending here means a
 * misbehaving client can't accidentally pass an empty string.
 *
 * @author Bruno Salmon
 */
final class UnsubscribePushNotificationsExecutor {

    private UnsubscribePushNotificationsExecutor() {}

    static Future<UnsubscribePushNotificationsResult> execute(
            UnsubscribePushNotificationsArgument arg) {
        boolean authenticated = ThreadLocalStateHolder.getUserId() != null;
        if (authenticated) {
            // A read-only session must not unsubscribe the borrowed account's devices: its claims
            // email is the TARGET's, so the write below would land on someone else's subscriptions.
            // The store update never passes the SQL submit layer's read-only gate, so refuse here,
            // synchronously, while the thread-local still holds the principal. The anonymous path
            // below is untouched — it carries no principal to be restricted.
            if (RestrictedPrincipalRegistry.isCurrentUserRestricted()) {
                return Future.failedFuture("[ReadOnlySessionError] This session is not allowed to change subscriptions");
            }
            // Use the auth-claim email, ignoring whatever the client passed.
            return AuthenticationService.getUserClaims()
                    .compose(claims -> {
                        String email = claims == null ? null : claims.email();
                        return unsubscribe(email);
                    });
        }
        // Anonymous path: trust the argument's email. Worst case (someone
        // unsubscribes a stranger's email) the impact is low — the stranger
        // just re-subscribes next time they need it.
        return unsubscribe(arg.email());
    }

    private static Future<UnsubscribePushNotificationsResult> unsubscribe(String email) {
        if (email == null || email.isBlank()) {
            return Future.failedFuture("No email to unsubscribe");
        }
        WebPushSubscriptionStore store = getStore();
        if (store == null) {
            return Future.failedFuture("No WebPushSubscriptionStore registered");
        }
        return store.unsubscribeByEmail(email)
                .map(count -> {
                    Console.log("[UnsubscribePushNotifications] email=" + email
                            + " → disabled " + count + " row(s)");
                    return new UnsubscribePushNotificationsResult(email, count);
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
