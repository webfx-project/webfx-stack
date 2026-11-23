package dev.webfx.stack.db.query.cli;

import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.spi.impl.json.client.websocket.WebSocketBus;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.ServerCallerParameters;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.remote.LocalOrRemoteQueryServiceProvider;

import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * Whether a probe issued by this process really travels the path a browser app's query travels.
 *
 * <h3>Why this is a check and not a comment</h3>
 *
 * <p>The four controls this instrument exists to measure — the client query guard, the read deny list, the
 * read scope and the server-supplied caller parameters — all live on <em>one</em> seam:
 * {@code ExecuteQueryMethodEndpoint}, the bus address a client's query arrives at. A probe that reaches the
 * database by any other route passes none of them, and would report "the caller could read N rows" having
 * never asked the question the controls answer. It would not fail; it would succeed, with a number, which is
 * the worst available outcome.
 *
 * <p>Three different mistakes put a probe on a different route, and none of them is visible in the probe's
 * own code:
 *
 * <ul>
 *   <li><b>A local data source.</b> {@code LocalQueryServiceProvider.executeQuery} — which the remote provider
 *       extends — looks for a locally connected provider FIRST and only falls through to the bus when there is
 *       none. Add a JDBC plugin and a datasource configuration to this CLI's classpath and every probe
 *       silently becomes a direct database query, with the bus endpoint never entered. This is the real
 *       bypass, and the only one that could happen by accident.
 *       <p>It is also what keeps the DQL compiler out of the way. {@code DqlQueryInterceptorInitializer} is
 *       on this tool's classpath — WebFX auto-injects it into any executable that uses both
 *       {@code dev.webfx.stack.db.query} and {@code dev.webfx.stack.orm.domainmodel} — and it intercepts
 *       every query to translate DQL into SQL. It does that <em>only</em> when the data source is local,
 *       which is the same condition this test asks about. So one answer covers both: with no local data
 *       source, the interceptor passes the statement through untouched and the server receives the DQL the
 *       probe wrote. With one, the probe would be compiled here, executed here, and would pass through no
 *       endpoint at all.</li>
 *   <li><b>A server-side query provider.</b> If this module were ever built alongside a server query
 *       implementation, the service loader could hand back a provider that executes in process.
 *       <p><b>Ask the service loader, not {@code QueryService.getProvider()}.</b> After a boot the latter
 *       returns an INTERCEPTOR — {@code DqlQueryInterceptorInitializer} wraps the provider in a lambda at
 *       job-init time, in every client build — so an {@code instanceof} against it fails and would refuse
 *       every probe. (It did: the first boot of this tool refused itself.) The declared implementation is
 *       what answers "where would a query go", and interceptors do not change it. Nor do they need a test of
 *       their own here: the only one in this build translates DQL to SQL exactly when the data source is
 *       local, which is the condition the next bullet already refuses on.</p></li>
 *   <li><b>A bus that is not a network bus.</b> A simple or local bus delivers to handlers registered in this
 *       JVM. There are none, so a probe would hang rather than lie — but "hangs" and "measured nothing" are
 *       close enough that it is worth naming.</li>
 * </ul>
 *
 * <h3>And two ways a probe refuses itself</h3>
 *
 * <p>{@link #refusalFor(QueryArgument)} covers the other half: a probe that the client query guard would
 * refuse for a reason of its own tells us nothing about read scope. A statement with no language is refused as
 * raw SQL, and an argument naming a {@code caller.*} parameter is refused as supplying a server-supplied
 * value. Both are the CLI's fault, both come back as a bare string from the bus, and a run that reported them
 * as "refused" alongside a genuine scope refusal would be reporting two different things in one word. So the
 * CLI checks its own argument before sending it, and from then on a refusal is about the caller.
 *
 * <h3>What it deliberately does NOT assert</h3>
 *
 * <p>That the controls are installed, or that they work. The CLI is the thing that measures them; asserting
 * their behaviour here would be asserting the answer. All this says is that the question was asked at the
 * right door.
 *
 * @author Claude Code
 */
public final class ClientPath {

    private ClientPath() {}

    /**
     * Why a probe from this process would not travel the ordinary client path, or null when it would.
     *
     * <p>Call it once, after the runtime has booted and before the first probe. A non-null answer is a refusal
     * to run, not a warning: every number this tool could produce in that state would be about the wrong
     * thing.
     *
     * @param dataSourceId the data source the probes will name, since locality is per data source
     * @param bus          the bus the probes will travel, as {@code BusService.bus()} returns it
     */
    /**
     * The query service implementation this build DECLARES, ignoring any interceptor installed over it.
     *
     * <p>A fresh {@code ServiceLoader} lookup rather than {@code QueryService.getProvider()}, for the reason
     * in the class comment: the second answers with the DQL interceptor's lambda once the platform has booted.
     */
    public static QueryServiceProvider declaredQueryServiceProvider() {
        for (QueryServiceProvider provider : ServiceLoader.load(QueryServiceProvider.class))
            return provider;
        return null;
    }

    public static String refusal(Object dataSourceId, Bus bus, QueryServiceProvider queryServiceProvider) {
        return refusal(dataSourceId, bus, queryServiceProvider,
                       LocalDataSourceService::isDataSourceLocal,
                       candidate -> candidate instanceof WebSocketBus);
    }

    /**
     * The same decision with its two environment questions passed in, so both answers to each can be
     * exercised without a running platform.
     *
     * <p>Neither is a seam for production to use: the public overload above supplies the real ones, and they
     * are the whole content of this method's contact with the outside world. Injecting them here is what lets
     * {@code ClientPathCheck} assert that this returns null when the path IS clean — which an {@code
     * instanceof} against a class a test cannot instantiate would make untestable, and an untestable "all
     * clear" is how a guard that always refuses, or never does, goes unnoticed.
     *
     * @param dataSourceIsLocal whether that data source is connected inside this process
     * @param isNetworkBus      whether that bus sends to a server rather than delivering in process
     */
    static String refusal(Object dataSourceId, Bus bus, QueryServiceProvider queryServiceProvider,
                          Predicate<Object> dataSourceIsLocal, Predicate<Bus> isNetworkBus) {
        if (queryServiceProvider == null)
            return "No query service provider is registered, so a probe has nowhere to go.";
        if (!(queryServiceProvider instanceof LocalOrRemoteQueryServiceProvider))
            return "The registered query service provider is " + queryServiceProvider.getClass().getName()
                   + ", which is not the remote one. A probe would not reach the client query endpoint,"
                   + " so it would pass none of the controls this tool measures.";
        if (dataSourceId != null && dataSourceIsLocal.test(dataSourceId))
            return "Data source " + dataSourceId + " is LOCAL to this process, so the remote provider would"
                   + " execute probes against the database directly and never enter the client query endpoint."
                   + " Remove the local data source configuration (and any JDBC plugin) from this tool.";
        if (bus == null)
            return "No bus is available, so a probe has nothing to travel over.";
        if (!isNetworkBus.test(bus))
            return "The bus is " + bus.getClass().getName() + ", not a websocket bus. A probe would be"
                   + " delivered inside this process rather than sent to the server.";
        return null;
    }

    /**
     * Why this argument is not a client-path probe, or null when it is.
     *
     * <p>Both refusals mirror {@code ClientQueryGuard} exactly, and deliberately re-state rather than call it:
     * the guard runs on the server, this runs before anything is sent, and the point is to tell the CLI's own
     * mistakes apart from the server's answers.
     */
    public static String refusalFor(QueryArgument argument) {
        if (argument == null)
            return "There is no query argument to send.";
        if (argument.getLanguage() == null)
            return "This probe has no language set, so the server would refuse it as raw SQL before any read"
                   + " control saw it. A probe must be DQL.";
        if (ServerCallerParameters.suppliesServerName(argument.getParameterNames()))
            return "This probe names a parameter the server alone may supply (one beginning 'caller.'), so the"
                   + " server would refuse it outright. The caller's own values are attached by the endpoint;"
                   + " a probe asks for them by writing the term in the statement, never by binding a value.";
        return null;
    }
}
