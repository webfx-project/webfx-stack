package dev.webfx.stack.authz.core;

import java.util.Map;

/**
 * Implemented by an authorization request that knows the context it should be judged in.
 *
 * <p>Rules can be scoped to a context — {@code context:organizationId=42} grants only within that
 * organisation — and something has to say what the caller's context IS. On a client that is ambient:
 * one user, one screen, one organisation being looked at. On a server it belongs to the request, and
 * two requests being handled at the same moment may be in different contexts.
 *
 * <p><b>The context must describe the target, not the caller's claim about it.</b> A request that
 * simply echoes back an organisation id it was given proves nothing: a caller granted in organisation
 * 42 could then name 42 while acting on a row belonging to 7. Resolve the target's real owner —
 * usually one indexed lookup — and report that.
 *
 * @author Bruno Salmon
 */
public interface HasAuthorizationContext {

    /**
     * The context this request is judged in. Never null; return an empty map when there is no context,
     * which matches only rules that declare none.
     */
    Map<String, String> getAuthorizationContext();

}
