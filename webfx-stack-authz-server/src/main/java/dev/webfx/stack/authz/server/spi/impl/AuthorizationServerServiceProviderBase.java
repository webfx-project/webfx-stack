package dev.webfx.stack.authz.server.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authz.core.HasAuthorizationContext;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRuleRegistry;
import dev.webfx.stack.authz.server.spi.AuthorizationServerServiceProvider;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side counterpart of the client's provider base: keeps one rule registry per principal and
 * answers authorization questions from it.
 *
 * <p>Deliberately mirrors {@code AuthorizationClientServiceProviderBase} — same caching shape, same
 * per-user keying — with three differences that come from being on the other side of the wire.
 *
 * <p><b>The principal is read per request, not from a UI singleton.</b> The client resolves one logged
 * in user from {@code FXUserId}; a server has a different caller on every request and reads it from
 * {@link ThreadLocalStateHolder}. That read happens synchronously, on the calling thread, before the
 * registry lookup that may go to the database — the thread-local is restored when the synchronous
 * portion returns, so a later read would see nobody and answer for nobody.
 *
 * <p><b>Building a registry is asynchronous.</b> The client has its grants pushed to it; a server has
 * to compute them, which is queries. So the cache holds futures, which also dedupes: a hundred requests
 * arriving for the same user while the first computation is in flight all wait on it rather than
 * starting a hundred more.
 *
 * <p><b>The cache is concurrent.</b> The client's is guarded by a lock because a UI has one thread that
 * matters; enforcement runs on whatever thread is handling the request. Lookup-then-insert is not
 * atomic here, so two threads can briefly build the same registry — wasteful, never wrong, and rarer
 * than the lock contention that serialising every check would cost instead.
 *
 * <p>Failure is denial. If the grants cannot be computed, the answer is no: an authorization question
 * that cannot be answered has not been answered in the affirmative.
 *
 * @author Bruno Salmon
 */
public abstract class AuthorizationServerServiceProviderBase implements AuthorizationServerServiceProvider {

    private record RegistryKey(Object principal, boolean backoffice) {}

    private record CachedRegistry(Future<InMemoryAuthorizationRuleRegistry> future, long computedAtMillis) {}

    /**
     * How long a principal's parsed rules may outlive a change to them.
     *
     * <p>This cache had no expiry at all, which meant a REVOKED privilege kept working on any instance
     * that had already cached that principal — not for a window, but until the process restarted. That
     * was invisible while nothing enforced anything; the moment enforcement is real it is the difference
     * between a revocation and a suggestion.
     *
     * <p>Invalidation on an authorization write (see {@link #invalidateAllRuleRegistries()}) makes the
     * change immediate on the instance that SAW the write. This TTL is what bounds it everywhere else:
     * production runs more than one instance, and the one that did not serve the write has no idea
     * anything changed. Belt and braces, in that order — the fast path for the common case, a bounded
     * worst case for the rest.
     */
    private static final long REGISTRY_CACHE_TTL_MILLIS = 120_000;

    private final Map<RegistryKey, CachedRegistry> registryCache = new ConcurrentHashMap<>();

    @Override
    public Future<Boolean> isAuthorized(Object operationAuthorizationRequest) {
        // Everything the answer depends on is read HERE, on the calling thread, while the request's
        // state is still in place. Past the compose() below there is no principal to read.
        Object userId = normalisedPrincipal(ThreadLocalStateHolder.getUserId());
        boolean backoffice = ThreadLocalStateHolder.isBackoffice();
        Map<String, String> context = contextOf(operationAuthorizationRequest);
        return getOrCreateRuleRegistry(userId, backoffice)
            .map(registry -> registry.doesRulesAuthorize(operationAuthorizationRequest, context))
            .recover(failure -> Future.succeededFuture(false));
    }

    /**
     * The context to judge this request in, taken from the request itself when it knows.
     *
     * <p>A request that does not implement {@link HasAuthorizationContext} is judged with no context,
     * which matches only rules that declare none — so a context-scoped grant does NOT apply by default.
     * That is the safe direction: forgetting to report a context withholds grants rather than
     * conferring them.
     */
    protected Map<String, String> contextOf(Object operationAuthorizationRequest) {
        if (operationAuthorizationRequest instanceof HasAuthorizationContext hac) {
            Map<String, String> context = hac.getAuthorizationContext();
            if (context != null)
                return context;
        }
        return Collections.emptyMap();
    }

    /** Public and logged-out callers share one registry, as they share one grant set. */
    protected Object normalisedPrincipal(Object userId) {
        return LogoutUserId.isLogoutUserIdOrNull(userId) ? LogoutUserId.LOGOUT_USER_ID : userId;
    }

    protected Future<InMemoryAuthorizationRuleRegistry> getOrCreateRuleRegistry(Object userId, boolean backoffice) {
        RegistryKey key = new RegistryKey(userId, backoffice);
        long now = System.currentTimeMillis();
        CachedRegistry cached = registryCache.get(key);
        // Reusable while still in flight (so concurrent checks share one computation) or young enough.
        if (cached != null && (!cached.future().isComplete()
                               || cached.future().succeeded() && now - cached.computedAtMillis() < REGISTRY_CACHE_TTL_MILLIS))
            return cached.future();
        Future<InMemoryAuthorizationRuleRegistry> future = createUserRuleRegistry(key.principal(), key.backoffice());
        CachedRegistry entry = new CachedRegistry(future, now);
        registryCache.put(key, entry);
        // A failed computation must not be served to the next caller — drop it so the next one retries.
        future.onFailure(e -> registryCache.remove(key, entry));
        return future;
    }

    /** Discard a principal's cached grants, for use when they are known to have changed. */
    protected void invalidateRuleRegistry(Object userId) {
        Object principal = normalisedPrincipal(userId);
        registryCache.keySet().removeIf(k -> java.util.Objects.equals(k.principal(), principal));
    }

    /**
     * Discard every cached registry, for when the rules themselves changed rather than one principal's.
     *
     * <p>All of them, because a row in the authorization tables does not say whom it affects: a rule
     * attached to a role reaches everyone holding that role, and working out who that is would mean
     * running the very queries this cache exists to avoid. Flushing is cheap and correct, and
     * authorization edits are rare — the expensive option here would be the clever one.
     */
    public void invalidateAllRuleRegistries() {
        registryCache.clear();
    }

    /**
     * Build the rule registry for this principal: fetch their grants and parse them, with whichever
     * rule parsers the application understands. Called at most once per principal per cache miss.
     */
    protected abstract Future<InMemoryAuthorizationRuleRegistry> createUserRuleRegistry(Object userId, boolean backoffice);

}
