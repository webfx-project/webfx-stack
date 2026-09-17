package dev.webfx.stack.webpush.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.webpush.WebPushSubscription;

import java.time.Instant;
import java.util.List;

/**
 * Storage SPI for subscription queries and rotation. Implementations bridge
 * the {@code webfx-stack-webpush-server} module — which knows nothing about
 * domain entities — to whatever ORM / table the host application uses for
 * push subscriptions and their recipient links.
 * <p>
 * Two responsibilities:
 * <ul>
 *   <li>{@link #rotate} — used by the SW's rotate-subscription REST handler
 *       when the browser hands us a new endpoint for an existing device.</li>
 *   <li>{@link #findRecipients} — used by the {@code SendPushNotification}
 *       BusCall to resolve which subscriptions a broadcast should reach,
 *       given an opaque host-defined {@code target} object.</li>
 * </ul>
 * <p>
 * The implementation is discovered via {@link java.util.ServiceLoader}.
 *
 * @author Bruno Salmon
 */
public interface WebPushSubscriptionStore {

    /**
     * Atomically rotate a subscription identified by its old endpoint.
     * <p>
     * Implementations should:
     * <ol>
     *   <li>Find the subscription row whose endpoint equals {@code oldEndpoint}.</li>
     *   <li>If found: update {@code endpoint}, {@code p256dhKey}, {@code authKey},
     *       {@code userAgent}, and {@code lastSeenAt} in one transaction, then
     *       complete the returned Future with {@code true}.</li>
     *   <li>If not found: complete the Future with {@code false}.</li>
     *   <li>On any database error: fail the Future.</li>
     * </ol>
     *
     * @return {@code true} if a row was updated, {@code false} if no row matched.
     */
    Future<Boolean> rotate(
            String oldEndpoint,
            String newEndpoint,
            String newP256dhKey,
            String newAuthKey,
            String userAgent,
            Instant lastSeenAt
    );

    /**
     * Soft-unsubscribe every active recipient row matching {@code email} —
     * implementations should set an "unsubscribed-at" timestamp (or
     * equivalent inactive marker) rather than DELETE, so opt-out rates stay
     * queryable for statistics. Idempotent: re-running on already-unsubscribed
     * rows is a no-op and should not be counted in the result.
     *
     * @return the number of rows newly marked as unsubscribed (zero if the
     *     email had no active opt-ins). Surfaces to the operator as "you
     *     unsubscribed from N notification channels".
     */
    Future<Integer> unsubscribeByEmail(String email);

    /**
     * Resolve the set of subscriptions a broadcast should reach.
     *
     * @param target      An opaque host-defined object describing which
     *                    recipients are in scope. The webfx-stack module
     *                    never inspects this — it's passed through verbatim
     *                    from the BO client (deserialised via the host's
     *                    own SerialCodec, e.g. modality's {@code
     *                    ModalityWebPushTarget}). The store impl is expected
     *                    to cast to its known target type and translate
     *                    into a query.
     * @param emailFilter When non-null, narrow the result to subscriptions
     *                    whose recipient row's email equals this value —
     *                    used for "test-send to my own devices" so the
     *                    operator can preview the message before broadcasting.
     *                    When null, no email filter is applied.
     * @return The subscriptions matching the criteria. Empty list (not null)
     *         when no recipients match. Implementations should propagate DB
     *         errors via Future failure rather than swallowing.
     */
    Future<List<WebPushSubscription>> findRecipients(Object target, String emailFilter);
}
