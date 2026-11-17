package dev.webfx.stack.authz.server;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * How an authorization question is asked, beyond "in the request's own context, from the cached grants".
 *
 * <ul>
 *   <li>{@link #inSomeContextKeyedBy}: <b>held somewhere</b> — for a request that has no target to take a context
 *       from. Granted when the context-free rules grant it, or when it is granted in a registered context whose
 *       keys are exactly the given ones. {@code inSomeContextKeyedBy("organizationId")} asks "held for a whole
 *       organization, in some organization": a grant scoped to one event (or event type) is not an answer about a
 *       whole organization, so it does not count.</li>
 *   <li>{@link #fresh}: <b>from the grants as they are now</b>, recomputed for this one question rather than read
 *       from a cache that may be up to its TTL old. For irreversible actions, where a revocation must take effect on
 *       the next call. It costs the queries the cache exists to save, so it is not for anything frequent.</li>
 * </ul>
 *
 * <p>Immutable; the two combine ({@code inSomeContextKeyedBy("organizationId").fresh()}). A provider that does not
 * implement a check refuses it: an option it ignored would be answered as a question it was not asked.
 *
 * @author Bruno Salmon
 */
public final class AuthorizationCheck {

    /** In the request's own context, from the cached grants: what {@code isAuthorized(request)} asks. */
    public static final AuthorizationCheck DEFAULT = new AuthorizationCheck(null, false);

    private final Set<String> someContextKeys;
    private final boolean fresh;

    private AuthorizationCheck(Set<String> someContextKeys, boolean fresh) {
        this.someContextKeys = someContextKeys;
        this.fresh = fresh;
    }

    /**
     * Held in some context keyed by exactly these keys (or by no context at all). At least one key: "some context
     * keyed by nothing" is the context-free question, which {@link #DEFAULT} with no context already asks.
     */
    public static AuthorizationCheck inSomeContextKeyedBy(String... contextKeys) {
        if (contextKeys.length == 0)
            throw new IllegalArgumentException("inSomeContextKeyedBy needs at least one context key");
        return new AuthorizationCheck(Set.of(contextKeys), false);
    }

    /** The same check, answered from grants recomputed for it. */
    public AuthorizationCheck fresh() {
        return new AuthorizationCheck(someContextKeys, true);
    }

    public boolean isFresh() {
        return fresh;
    }

    /** Null when the request is judged in its own context. */
    public Set<String> getSomeContextKeys() {
        return someContextKeys;
    }

    /** Which registered contexts count for {@link #inSomeContextKeyedBy}: those keyed by exactly its keys. */
    public Predicate<Map<String, String>> someContextFilter() {
        return someContextKeys == null ? null : context -> context.keySet().equals(someContextKeys);
    }

    public boolean isDefault() {
        return someContextKeys == null && !fresh;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AuthorizationCheck that && fresh == that.fresh && Objects.equals(someContextKeys, that.someContextKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(someContextKeys, fresh);
    }

    @Override
    public String toString() {
        return "AuthorizationCheck{" + (someContextKeys == null ? "own context" : "some context keyed by " + someContextKeys)
               + (fresh ? ", fresh" : "") + "}";
    }
}
