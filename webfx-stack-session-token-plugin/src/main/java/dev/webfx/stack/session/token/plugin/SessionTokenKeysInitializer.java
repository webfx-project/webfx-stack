package dev.webfx.stack.session.token.plugin;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.meta.Meta;
import dev.webfx.platform.substitution.Substitutor;
import dev.webfx.stack.session.token.IdentityTokenPolicy;
import dev.webfx.stack.session.token.SessionLifetime;
import dev.webfx.stack.session.token.SessionTier;
import dev.webfx.stack.session.token.SignedToken;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Reads the session-token signing keys from configuration and installs them.
 *
 * <p>Kept apart from {@link SignedToken} so the primitive depends on nothing and can be exercised
 * without a configured stack — the arrangement that lets its adversarial check run from a bare main().
 * This is the half that knows about configuration; that half knows about signing.
 *
 * <p>The current key mints and verifies; a previous key, when present, verifies only. That is what makes
 * a rotation survivable: publish a new key, keep the old one until every token minted under it has
 * expired, then drop it. With a single key every live session dies at the moment of rotation, and a
 * rotation that signs out every user is one that does not get performed.
 *
 * @author Bruno Salmon
 */
public final class SessionTokenKeysInitializer implements ApplicationJob {

    private static final String CONFIG_PATH = "webfx.stack.session.token";
    /** HMAC-SHA256 territory: a key shorter than its 256-bit block buys nothing and hides that it hasn't. */
    private static final int MINIMUM_KEY_BYTES = 32;
    private static final String REQUIRED_KEY = "required";
    private static final String LIFETIME_SCALE_KEY = "lifetimeScale";

    @Override
    public void onInit() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, this::onConfigLoaded);
    }

    private void onConfigLoaded(Config config) {
        List<byte[]> keys = new ArrayList<>(2);
        addKeyIfPresent(keys, config == null ? null : config.getString("signingKey"), "signingKey");
        addKeyIfPresent(keys, config == null ? null : config.getString("previousSigningKey"), "previousSigningKey");
        SignedToken.setKeys(keys);
        if (keys.isEmpty())
            // Loud, because the consequence is silent: with no key, identities cannot be minted, and
            // whatever depends on them falls back to whatever it does when nobody is identified.
            Console.log("⚠️ No session token signing key configured (" + CONFIG_PATH
                        + ".signingKey) — identity tokens cannot be minted or verified");
        else
            Console.log("🔑 Session token signing keys installed (" + keys.size()
                        + (keys.size() == 1 ? " key)" : " keys — a rotation is in progress)"));
        applyRequiredFlag(config, !keys.isEmpty());
        applyLifetimeScale(config);
    }

    private void applyLifetimeScale(Config config) {
        applyLifetimeScale(config == null ? null : config.getString(LIFETIME_SCALE_KEY), Meta.isDevelopment(), Meta.getEnvironment());
    }

    /**
     * Shortens every session lifetime by a factor, for testing the policy without waiting a day for it.
     *
     * <p><b>Honoured only in a development build, and that guard is the point.</b> {@link Meta#isDevelopment()}
     * reads the environment BAKED INTO THE BUILD by its Maven profile, not anything supplied at run time — so
     * on a server built for staging or production, no environment variable, conf file or deployment setting
     * can switch this on. Setting it there is refused out loud rather than obeyed quietly, because a test
     * value is exactly the kind of thing that reaches a real deployment by being copied from somebody's run
     * configuration.
     *
     * <p>Know where that guard stops: Maven's DEFAULT environment is development, so a jar built with no
     * profile at all counts as a development build and would honour the setting. The deploy workflows always
     * name one; a server built by hand for a real deployment must too.
     *
     * <p>And even where it is honoured, it can only SHORTEN — {@link SessionLifetime#setScale} refuses a
     * factor above 1. So the worst any misuse can do is sign people out sooner; it cannot weaken a bound.
     *
     * <p>Package-private, with the build's environment passed in, so LifetimeScaleGuardCheck can run it as a
     * production build would.
     */
    static void applyLifetimeScale(String configured, boolean developmentBuild, String environment) {
        if (configured == null || configured.isBlank() || !Substitutor.areValuesNonNullAndResolved(configured))
            return; // the overwhelmingly common case: nothing asked for, nothing said
        double factor;
        try {
            factor = Double.parseDouble(configured.trim());
        } catch (NumberFormatException e) {
            Console.log("⚠️ Ignoring " + CONFIG_PATH + "." + LIFETIME_SCALE_KEY + ": not a number");
            return;
        }
        if (factor == 1.0)
            return;
        if (!developmentBuild) {
            Console.log("🛑 IGNORED " + CONFIG_PATH + "." + LIFETIME_SCALE_KEY + " = " + factor + " — lifetime"
                        + " scaling is a test aid and is only honoured in a development build (this is '"
                        + environment + "'). Sessions keep their real lifetimes.");
            return;
        }
        double applied;
        try {
            applied = SessionLifetime.setScale(factor);
        } catch (IllegalArgumentException e) {
            Console.log("🛑 Refusing " + CONFIG_PATH + "." + LIFETIME_SCALE_KEY + ": " + e.getMessage()
                        + " — it may only shorten sessions, never lengthen them");
            return;
        }
        // Loud and specific: somebody testing needs the actual numbers, and anybody who finds this line in a
        // log they did not expect it in needs to know at once that lifetimes are not what they think.
        Console.log("⏱️ SESSION LIFETIMES SCALED by " + applied + (applied != factor ? " (clamped from " + factor + ")" : "")
                    + " — TEST VALUES, development only. access window " + seconds(SessionLifetime.accessWindowMillis())
                    + "; back office idle " + seconds(SessionLifetime.idleWindowMillis(SessionTier.BACK_OFFICE))
                    + " / absolute " + seconds(SessionLifetime.absoluteLifetimeMillis(SessionTier.BACK_OFFICE))
                    + "; front office idle " + seconds(SessionLifetime.idleWindowMillis(SessionTier.FRONT_OFFICE))
                    + " / absolute " + seconds(SessionLifetime.absoluteLifetimeMillis(SessionTier.FRONT_OFFICE))
                    + "; reuse grace " + seconds(SessionLifetime.REUSE_GRACE_MILLIS) + " (not scaled)."
                    // A session opened before this boot keeps the absolute bound it was opened with — the store
                    // hands it back on every renewal — so without this, the cap is tested on a session it
                    // does not apply to and the banner that should come never does.
                    + " Applies to sessions signed in from now on: one already open keeps the absolute bound it"
                    + " began with, so sign in again to test the cap.");
    }

    private static String seconds(long millis) {
        return millis >= 3_600_000 ? String.format("%.1fh", millis / 3_600_000.0)
             : millis >= 60_000 ? String.format("%.1fmin", millis / 60_000.0)
             : (millis / 1000) + "s";
    }

    /**
     * Applies the flip, and refuses to run a combination that would lock everyone out.
     *
     * <p><b>Required with no usable key logs out every user of this server</b>, including whoever would
     * fix it: nothing can be minted, so nothing verifies, so every identity is refused — and the process
     * otherwise looks perfectly healthy, which is what makes it dangerous. Under blue/green that instance
     * passes its health check and takes production traffic. So this refuses to start instead. A server
     * that will not come up is a deploy that visibly fails; a server that comes up and rejects everybody
     * is an outage someone has to diagnose.
     *
     * <p>The exception is the belt and the log line is the braces, deliberately in that order: whether a
     * throw from this callback aborts the boot chain depends on the caller, and this has not been proved
     * here, so the message is written to be unmissable on its own.
     */
    private void applyRequiredFlag(Config config, boolean hasKeys) {
        String configuredValue = config == null ? null : config.getString(REQUIRED_KEY);
        // Unset is the safe answer and also the common one: an environment that has never heard of this
        // setting keeps accepting bare claims, exactly as it did before the token existed. An unresolved
        // ${{ }} template is treated the same way — see the note on defaults in the declaration file.
        boolean required = configuredValue != null
                           && Substitutor.areValuesNonNullAndResolved(configuredValue)
                           && "true".equalsIgnoreCase(configuredValue.trim());
        if (required && !hasKeys) {
            String message = "FATAL: " + CONFIG_PATH + "." + REQUIRED_KEY + " is on but no signing key is"
                             + " usable — every user would be refused. Set " + CONFIG_PATH + ".signingKey,"
                             + " or turn " + REQUIRED_KEY + " off.";
            Console.log("🛑 " + message);
            throw new IllegalStateException(message);
        }
        IdentityTokenPolicy.setTokenRequired(required);
        if (required)
            Console.log("🛡 Identity tokens are REQUIRED — a claimed user id with no valid token is refused");
        else
            // Said out loud on every boot, because the quiet failure this whole mechanism guards against is
            // shipping it, believing it is on, and never checking. Silence here would read as success.
            Console.log("🔓 Identity tokens are accepted but NOT required — a client's claimed user id is"
                        + " still trusted on its own (" + CONFIG_PATH + "." + REQUIRED_KEY + " is off)");
    }

    /**
     * Decodes one configured key, refusing rather than weakening.
     *
     * <p>A key that is absent, unreadable or too short is dropped with a reason. It is deliberately not
     * padded, hashed or otherwise rescued into usability: silently accepting a four-character key would
     * produce a server that mints tokens, verifies them, looks entirely healthy, and protects nothing.
     * The value itself is never logged — only whether it was usable.
     */
    private void addKeyIfPresent(List<byte[]> keys, String configuredKey, String keyName) {
        if (configuredKey == null || configuredKey.isBlank())
            return;
        // An unresolved ${{ VAR }} comes back as the literal template text, not as null. Left alone it
        // is 38 printable characters, which clears the length bar below and becomes a perfectly usable
        // HMAC key made of the words "SESSION_TOKEN_PREVIOUS_SIGNING_KEY" — a server signing with a
        // constant that anyone reading this file can reproduce, while reporting a rotation that is not
        // happening. Treated as absent, which is what an unset variable means.
        if (!Substitutor.areValuesNonNullAndResolved(configuredKey)) {
            Console.log("⚠️ Ignoring " + CONFIG_PATH + "." + keyName + ": its variable is not set");
            return;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configuredKey.trim());
        } catch (IllegalArgumentException e) {
            // Not base64: treat the raw text as bytes rather than refuse outright, but hold it to the
            // same length bar below, so a pasted-wrong value fails on length instead of passing quietly.
            decoded = configuredKey.trim().getBytes(StandardCharsets.UTF_8);
        }
        if (decoded.length < MINIMUM_KEY_BYTES) {
            Console.log("⚠️ Ignoring " + CONFIG_PATH + "." + keyName + ": needs at least "
                        + MINIMUM_KEY_BYTES + " bytes, got " + decoded.length);
            return;
        }
        keys.add(decoded);
    }
}
