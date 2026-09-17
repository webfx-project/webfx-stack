package dev.webfx.stack.webpush.impl;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.webpush.WebPushConfig;
import dev.webfx.stack.webpush.WebPushPayload;
import dev.webfx.stack.webpush.WebPushResult;
import dev.webfx.stack.webpush.WebPushSubscription;
import dev.webfx.stack.webpush.spi.WebPushServerServiceProvider;

import java.nio.charset.StandardCharsets;

/**
 * Default {@link WebPushServerServiceProvider} implementation. Orchestrates
 * the four steps each push requires: serialise the payload to JSON, encrypt
 * it for the subscription's keys, build a signed VAPID-authorised HTTP
 * request, and map the response.
 *
 * @author Bruno Salmon
 */
public final class WebPushServiceImpl implements WebPushServerServiceProvider {

    private final String vapidPublicKey;
    private final PayloadEncryptor encryptor;
    private final PushServiceClient client;

    public WebPushServiceImpl(WebPushConfig config) {
        this.vapidPublicKey = config.publicKey();
        VapidSigner signer = new VapidSigner(
                config.publicKey(), config.privateKey(), config.subject());
        this.encryptor = new PayloadEncryptor();
        this.client = new PushServiceClient(signer);
    }

    @Override
    public String currentVapidPublicKey() {
        return vapidPublicKey;
    }

    @Override
    public Future<WebPushResult> send(WebPushSubscription subscription, WebPushPayload payload) {
        byte[] payloadJsonBytes = payloadToJson(payload).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptor.encrypt(
                payloadJsonBytes,
                subscription.p256dhKey(),
                subscription.authKey());
        return client.post(subscription.endpoint(), encrypted, payload);
    }

    /**
     * Serialises the payload to the JSON shape the front-end service worker
     * expects (see {@code sw-core.ts}'s {@code push} handler in the React app).
     * Only the fields the SW reads are included; everything else is omitted.
     */
    private static String payloadToJson(WebPushPayload payload) {
        AstObject json = AST.createObject();
        if (payload.title() != null) json.set("title", payload.title());
        if (payload.body() != null)  json.set("body", payload.body());
        if (payload.url() != null)   json.set("url", payload.url());
        if (payload.tag() != null)   json.set("tag", payload.tag());
        return AST.formatObject(json, "json");
    }
}
