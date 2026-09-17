package dev.webfx.stack.webpush.handler;

import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.router.spi.impl.vertx.VertxRoutingContext;
import dev.webfx.stack.webpush.rest.WebPushServerRestModuleBooter;
import dev.webfx.stack.webpush.spi.WebPushSubscriptionStore;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * REST handler for {@code POST /rest/push/rotate-subscription}. Invoked by the
 * front-end service worker's {@code pushsubscriptionchange} handler when the
 * browser rotates the push subscription's endpoint/keys (e.g. token rotation,
 * profile changes, OS updates).
 * <p>
 * Without this endpoint, every rotation event silently dies the subscription
 * on the server side — the next send attempt would return 410 Gone and the
 * user would stop receiving notifications until they re-subscribed through
 * the app. With it, the server keeps the subscription row alive across
 * rotations without any user interaction.
 *
 * <h2>Expected request body</h2>
 * <pre>{@code
 * {
 *   "old": { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } },
 *   "new": { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } },
 *   "userAgent":  "Mozilla/5.0 ...",
 *   "lastSeenAt": "2026-05-29T10:22:13.123Z"
 * }
 * }</pre>
 *
 * <h2>Responses</h2>
 * <ul>
 *   <li>{@code 200 OK} — subscription was found and updated</li>
 *   <li>{@code 400 Bad Request} — required fields missing or malformed JSON</li>
 *   <li>{@code 404 Not Found} — no subscription matches {@code old.endpoint}</li>
 *   <li>{@code 500 Internal Server Error} — store operation failed</li>
 * </ul>
 *
 * <h2>Platform leak</h2>
 * This class reaches into Vert.x's {@code RoutingContext} for body parsing
 * and for setting non-200 response status codes — the webfx-stack Router
 * abstraction doesn't expose either yet. It's the only intentional leak in
 * the module and a candidate for cleanup once those abstractions grow.
 *
 * @author Bruno Salmon
 */
public final class RotateSubscriptionHandler {

    private final WebPushSubscriptionStore store;

    public RotateSubscriptionHandler(WebPushSubscriptionStore store) {
        this.store = store;
    }

    /** Entry point — wired by {@link WebPushServerRestModuleBooter}. */
    public void handle(RoutingContext ctx) {
        io.vertx.ext.web.RoutingContext vertxCtx = ((VertxRoutingContext) ctx).getVertxRoutingContext();

        JsonObject body;
        try {
            body = vertxCtx.body().asJsonObject();
        } catch (Exception e) {
            respond(vertxCtx, 400, "Invalid JSON body");
            return;
        }
        if (body == null) {
            respond(vertxCtx, 400, "Missing request body");
            return;
        }

        // Extract + validate. We do the validation inline rather than as a
        // separate "parse to record" step because there's only one caller
        // (the SW) and the validation error returned is purely for diagnostics.
        JsonObject oldSub = body.getJsonObject("old");
        JsonObject newSub = body.getJsonObject("new");
        if (oldSub == null || newSub == null) {
            respond(vertxCtx, 400, "Missing 'old' or 'new' subscription");
            return;
        }
        String oldEndpoint = oldSub.getString("endpoint");
        String newEndpoint = newSub.getString("endpoint");
        JsonObject newKeys = newSub.getJsonObject("keys");
        if (oldEndpoint == null || newEndpoint == null || newKeys == null) {
            respond(vertxCtx, 400, "Missing endpoint or keys");
            return;
        }
        String newP256dh = newKeys.getString("p256dh");
        String newAuth   = newKeys.getString("auth");
        if (newP256dh == null || newAuth == null) {
            respond(vertxCtx, 400, "Missing p256dh or auth");
            return;
        }

        // These two are optional — if the SW doesn't send them, fall back to
        // sensible defaults (current server time for lastSeenAt, empty UA).
        String userAgent = body.getString("userAgent", "");
        Instant lastSeenAt = parseInstantOrNow(body.getString("lastSeenAt"));

        store.rotate(oldEndpoint, newEndpoint, newP256dh, newAuth, userAgent, lastSeenAt)
                .onSuccess(updated -> respond(vertxCtx, updated ? 200 : 404, null))
                .onFailure(err -> respond(vertxCtx, 500, "Rotate failed: " + err.getMessage()));
    }

    private static Instant parseInstantOrNow(String iso) {
        if (iso == null || iso.isEmpty()) return Instant.now();
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static void respond(io.vertx.ext.web.RoutingContext vertxCtx, int status, String body) {
        if (body == null) {
            vertxCtx.response().setStatusCode(status).end();
        } else {
            vertxCtx.response()
                    .setStatusCode(status)
                    .putHeader("content-type", "text/plain;charset=utf-8")
                    .end(body);
        }
    }
}
