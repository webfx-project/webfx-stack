package dev.webfx.stack.webpush;

import dev.webfx.platform.conf.Config;
import dev.webfx.platform.substitution.Substitutor;

/**
 * Configuration for Web Push (VAPID keys + contact subject).
 * <p>
 * Read from the runtime config tree at path {@code webpush.vapid} — typically
 * declared in the module's {@code declare@webpush.vapid.properties} (which
 * webfx:update merges into the application's {@code src-root.json}) with
 * {@code ${{ VAR }}} placeholders that are resolved at runtime against the
 * variables loaded from the deployment's config directory (e.g. {@code
 * /opt/kbs/conf/public-variables.properties} + {@code secret-variables.properties}).
 * <p>
 * <b>Do not read directly from {@code SourcesConfig}</b> — that returns the
 * raw classpath bundle without runtime overrides or variable substitution
 * and the {@code ${{ VAPID_* }}} placeholders would leak through verbatim.
 * Use {@link dev.webfx.platform.conf.ConfigLoader#onConfigLoaded(String,
 * java.util.function.Consumer)} with path {@link #CONFIG_PATH} and pass the
 * resulting subtree to {@link #from(Config)}.
 *
 * @author Bruno Salmon
 */
public final class WebPushConfig {

    public static final String CONFIG_PATH = "webpush.vapid";

    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public WebPushConfig(String publicKey, String privateKey, String subject) {
        this.publicKey  = publicKey;
        this.privateKey = privateKey;
        this.subject    = subject;
    }

    /**
     * Builds a {@link WebPushConfig} from the {@code webpush.vapid} subtree.
     * Returns {@code null} if the subtree is missing or the public/private
     * keys aren't both populated — callers should treat that as "Web Push
     * not configured" and skip wiring the service/routes rather than fail
     * the boot.
     */
    public static WebPushConfig from(Config config) {
        if (config == null) {
            return null;
        }
        String publicKey  = config.getString("publicKey");
        String privateKey = config.getString("privateKey");
        String subject    = config.getString("subject");
        if (!Substitutor.areValuesNonNullAndResolved(publicKey, privateKey)) {
            return null;
        }
        return new WebPushConfig(publicKey, privateKey, subject);
    }

    public String publicKey() {
        return publicKey;
    }

    public String privateKey() {
        return privateKey;
    }

    /**
     * The contact URI push service operators reach us through (per RFC 8292).
     * Must be a {@code mailto:} or {@code https:} URI. May be empty/null in
     * dev — the VAPID signer falls back to a placeholder in that case.
     */
    public String subject() {
        return subject;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
