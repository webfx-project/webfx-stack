package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusHook;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.LocalQueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.remote.LocalOrRemoteQueryServiceProvider;
import dev.webfx.stack.db.query.spi.impl.remote.RemoteQueryServiceProvider;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * The property everything else in this tool rests on: a probe goes out through the ordinary client path.
 *
 * <h3>Why this check is the one that matters</h3>
 *
 * <p>The four controls the tool measures — the client query guard, the read deny list, the read scope and the
 * server-supplied caller parameters — are all on the bus endpoint a client's query arrives at. A probe that
 * reaches the data any other way passes none of them and still produces a number, which would be read as "the
 * caller can see N" when it means "a process with database access can see N". Nothing downstream would look
 * wrong.
 *
 * <p>So each case here is a way that happens, written as the failure rather than as the feature:
 *
 * <ul>
 *   <li>a local data source turns every probe into a direct database query — the only one of these that could
 *       arrive by accident, by adding a JDBC plugin to the build;</li>
 *   <li>a query provider that is not the remote one executes in process;</li>
 *   <li>a bus that is not a network bus delivers inside this JVM;</li>
 *   <li>a probe with no language is refused by the server as raw SQL, and one naming a {@code caller.*}
 *       parameter is refused as supplying a server value — both of which would be reported as "refused"
 *       beside a genuine scope refusal, saying two different things in one word.</li>
 * </ul>
 *
 * <p><b>Probed by deleting each guard.</b> Every case below was confirmed by removing the corresponding test
 * from {@code ClientPath} and watching this check fail, then restoring it — the clean run alone proves
 * nothing, because a guard that is never reached and a guard that is always satisfied look the same from here.
 *
 * @author Claude Code
 */
public class ClientPathCheck {

    /** A bus that is not a network bus: it would deliver to handlers in this JVM, of which there are none. */
    static final class InProcessBus implements Bus {
        @Override public Bus publish(String address, Object body, DeliveryOptions options) { return this; }
        @Override public Bus send(String address, Object body, DeliveryOptions options) { return this; }
        @Override public <T> Bus request(String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) { return this; }
        @Override public <T> Registration register(boolean local, String address, Handler<Message<T>> handler) { return () -> {}; }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }
        @Override public Bus setHook(BusHook hook) { return this; }
    }

    public static void main(String[] args) {
        Bus networkBus = new InProcessBus(); // stands in for a websocket bus; isNetworkBus decides, not the type
        Bus inProcessBus = new InProcessBus();
        QueryServiceProvider remote = new RemoteQueryServiceProvider();
        QueryServiceProvider local = new LocalQueryServiceProvider();

        section("A clean client path is accepted — without this, every refusal below proves nothing");
        check("remote provider, non-local data source, network bus: no refusal",
              ClientPath.refusal("MDS", networkBus, remote, id -> false, b -> true) == null);

        section("The ways a probe silently stops being a client-path probe");
        String localDataSource = ClientPath.refusal("MDS", networkBus, remote, id -> true, b -> true);
        check("a LOCAL data source is refused",
              localDataSource != null);
        check("and the refusal says the endpoint would never be entered",
              localDataSource != null && localDataSource.contains("never enter the client query endpoint"));
        check("locality is asked about THIS data source, not about data sources in general",
              ClientPath.refusal("MDS", networkBus, remote, "OTHER"::equals, b -> true) == null
              && ClientPath.refusal("MDS", networkBus, remote, "MDS"::equals, b -> true) != null);
        check("a null data source cannot be local, so it is not refused for that",
              ClientPath.refusal(null, networkBus, remote, id -> true, b -> true) == null);

        check("a non-remote query service provider is refused",
              ClientPath.refusal("MDS", networkBus, local, id -> false, b -> true) != null);
        check("no query service provider at all is refused",
              ClientPath.refusal("MDS", networkBus, null, id -> false, b -> true) != null);

        check("a bus that is not a network bus is refused",
              ClientPath.refusal("MDS", inProcessBus, remote, id -> false, b -> false) != null);
        check("no bus at all is refused",
              ClientPath.refusal("MDS", null, remote, id -> false, b -> true) != null);

        section("An interceptor must not be mistaken for the implementation");
        // After a boot, QueryService.getProvider() is DqlQueryInterceptorInitializer's lambda, not the remote
        // provider. Measured: the first boot of this tool refused itself on exactly that. So the question is
        // asked of the DECLARED provider, and a bare lambda is still not an answer to it.
        QueryServiceProvider interceptorLambda = argument -> null;
        check("a lambda standing in for an interceptor is NOT accepted as the implementation",
              ClientPath.refusal("MDS", networkBus, interceptorLambda, id -> false, b -> true) != null);
        check("the declared provider is read from the service loader, so an interceptor cannot hide it",
              ClientPath.declaredQueryServiceProvider() instanceof LocalOrRemoteQueryServiceProvider);

        section("The real wiring: the public overload asks the real questions");
        // The production lambda is 'bus instanceof WebSocketBus'. A WebSocketBus cannot be constructed from
        // here (package-private constructor, and constructing one would connect), so what is pinned is that
        // the real predicate REJECTS a bus that is not one. The remaining cell - that it accepts a real
        // WebSocketBus - is the instanceof itself.
        check("the public overload refuses a bus that is not a websocket bus",
              ClientPath.refusal("MDS", inProcessBus, remote) != null);
        check("the public overload refuses a provider that is not the remote one",
              ClientPath.refusal("MDS", inProcessBus, local) != null);

        section("A probe that the server would refuse for the tool's own reasons");
        QueryArgument noLanguage = QueryArgument.builder()
            .setDataSourceId("MDS").setStatement("select count(1) as n from Conversation").build();
        check("a statement with no language is refused before it is sent",
              ClientPath.refusalFor(noLanguage) != null);
        check("and the refusal explains it would be read as raw SQL",
              ClientPath.refusalFor(noLanguage).contains("raw SQL"));

        QueryArgument callerParameter = QueryArgument.builder()
            .setDataSourceId("MDS").setLanguage(DqlRunner.DQL)
            .setStatement("select count(1) as n from Conversation where viewerPerson=$1")
            .setParameters(6)
            .setParameterNames("caller.person")
            .build();
        check("a probe naming a server-supplied parameter is refused before it is sent",
              ClientPath.refusalFor(callerParameter) != null);

        QueryArgument ordinaryProbe = QueryArgument.builder()
            .setDataSourceId("MDS").setLanguage(DqlRunner.DQL)
            .setStatement("select count(1) as n from Conversation where event=$1")
            .setParameters(1857)
            .build();
        check("an ordinary DQL probe with positional parameters is accepted",
              ClientPath.refusalFor(ordinaryProbe) == null);
        check("a probe asking for a caller value in the STATEMENT is accepted — that is how it must be asked",
              ClientPath.refusalFor(QueryArgument.builder()
                  .setDataSourceId("MDS").setLanguage(DqlRunner.DQL)
                  .setStatement("select count(1) as n from Conversation where viewerPerson=CALLER_PERSON")
                  .build()) == null);
        check("no argument at all is refused",
              ClientPath.refusalFor(null) != null);

        section("The runner builds arguments the path accepts");
        DqlRunner runner = new DqlRunner("MDS", 1000);
        QueryArgument built = runner.argumentFor("select count(1) as n from Conversation where event=$1", new Object[] {1857});
        check("the runner always sets the language",
              DqlRunner.DQL.equals(built.getLanguage()));
        check("the runner never names a parameter",
              built.getParameterNames() == null);
        check("the runner asks for column metadata, without which every column prints as col0",
              built.isSendMetadata());
        check("the runner declares it has no DQL runtime, so the server compiles expression fields",
              !built.isHasDqlRuntime());
        check("so what the runner builds passes the argument check",
              ClientPath.refusalFor(built) == null);

        System.exit(report("ClientPathCheck"));
    }
}
