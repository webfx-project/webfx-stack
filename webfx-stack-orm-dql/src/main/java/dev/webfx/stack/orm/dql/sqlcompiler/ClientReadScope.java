package dev.webfx.stack.orm.dql.sqlcompiler;

import dev.webfx.stack.orm.expression.Expression;

/**
 * The condition the server adds to a client's read of a given entity — data scope, the second mechanism of
 * the read-authorization plan.
 *
 * <p>Sits where {@code DenyingCompilerDomainModelReader} sits, and is total for the same reason: the compiler
 * creates one build per select, from a single place, so a rule attached to an entity applies wherever that
 * entity is selected — in a subquery, a CTE body, every branch of a union. A pass over the parsed statement
 * would be only as complete as its author's memory of the grammar.
 *
 * <p><b>The condition is an Expression, never SQL text.</b> Three things follow from that and none would
 * survive a string: it is compiled by the same compiler, so the denying reader still checks every name inside
 * it and a scope predicate cannot quietly read a denied column; its parameters bind through the ordinary path
 * rather than being spliced; and it stays portable across the DBMS syntaxes.
 *
 * <p><b>The token is opaque here.</b> DQL has no notion of a grant, so it is handed a value that identifies
 * WHICH scope applies and asks the provider what that means. That keeps the dependency pointing one way, and
 * it is what lets the compilation cache stay correct: compiled SQL is keyed by statement text, and with scope
 * injected the same text compiles differently per scope, so the token joins the key. It has to be a small
 * comparable value for that to be bounded — which is the argument for a fixed set of named scopes rather than
 * a predicate assembled per request, arriving here as a hard constraint rather than a preference.
 *
 * @author Claude Code
 */
public final class ClientReadScope {

    public interface Provider {
        /**
         * The condition to AND into reads of this domain class under this scope, or null for none.
         *
         * <p>Must be parsed against {@code domainClass}, and should be cheap: it is evaluated for every row
         * of every read of that entity. A membership test against ids materialised once per request beats a
         * correlated subquery, and the read path has form here — see the registration-rooms work.
         */
        Expression<?> readScopeCondition(Object domainClass, Object scopeToken);
    }

    /**
     * Volatile for the reason given on {@code ClientScopeToken.resolver}: {@link #conditionFor} injects
     * nothing when this is null, so a thread that cannot yet see the boot-time registration compiles the
     * read unscoped — the same silent answer, one layer down.
     */
    private static volatile Provider provider;

    public static void registerProvider(Provider provider) {
        ClientReadScope.provider = provider;
    }

    /**
     * Null when nothing is registered, no scope applies, or this entity is unscoped.
     *
     * <p>Public only because the two injection points are in different packages — the select root in this
     * one, the join in {@code ...sqlcompiler.sql} — and not part of the surface anyone outside the compiler
     * should call. Register a {@link Provider} instead.
     */
    public static Expression<?> conditionFor(Object domainClass, Object scopeToken) {
        return provider == null || scopeToken == null ? null : provider.readScopeCondition(domainClass, scopeToken);
    }
}
