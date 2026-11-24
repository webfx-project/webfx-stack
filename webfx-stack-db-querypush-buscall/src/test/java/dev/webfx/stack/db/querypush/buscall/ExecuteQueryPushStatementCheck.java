package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.query.ClientQueryGuard;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.ServerCallerParameters;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * That a push update which leaves out its statement is checked, and given the caller's values, as the full
 * query it stands for.
 *
 * <p>The Java client sends {@code statement = null} when only the parameters of an open stream change. From
 * 2026-09-18 the read guard compiled that null, threw, and refused every such update ("Cannot invoke
 * String.length() because s is null" in prod). Skipping the check for a null statement would have been
 * worse: the stored statement would have re-run without the caller's own values, which are attached only
 * to a statement that names them.
 *
 * <p>No test framework: runs from main() and exits non-zero on failure, like ServerCallerParametersCheck.
 */
public class ExecuteQueryPushStatementCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static final String STORED = "select id from Document d where person=CALLER_PERSON and event=$1";
    static final List<String> guarded = new ArrayList<>();         // statements the guard was asked about
    static final List<QueryArgument> executed = new ArrayList<>(); // query arguments the provider received
    static final List<Object> executedArgs = new ArrayList<>();

    public static void main(String[] args) {
        // Stream 7 belongs to client "owner"; the fake answers as the server provider does.
        QueryPushServiceProvider fake = (QueryPushServiceProvider) Proxy.newProxyInstance(
            QueryPushServiceProvider.class.getClassLoader(), new Class<?>[]{QueryPushServiceProvider.class},
            (proxy, method, a) -> {
                switch (method.getName()) {
                    case "getStreamStatement":
                        return Integer.valueOf(7).equals(a[0]) && "owner".equals(ThreadLocalStateHolder.getRunId()) ? STORED : null;
                    case "executeQueryPush":
                        QueryPushArgument qpa = (QueryPushArgument) a[0];
                        executedArgs.add(qpa);
                        executed.add(qpa.getQueryArgument());
                        return Future.succeededFuture(qpa.getQueryStreamId());
                    default:
                        return null;
                }
            });
        SingleServiceProvider.registerServiceProvider(QueryPushServiceProvider.class, fake);
        ClientQueryGuard.registerInspector(argument -> {
            guarded.add(argument.getStatement());
            return argument.getStatement() == null ? "would have thrown" : null;
        });
        ServerCallerParameters.registerResolver(name -> "caller.person".equals(name) ? 101 : null);
        ExecuteQueryPushMethodEndpoint endpoint = new ExecuteQueryPushMethodEndpoint();

        System.out.println("ExecuteQueryPushStatementCheck");

        // ── The bug: a parameters-only update of the caller's own stream ──
        Future<Object> f = call(endpoint, "owner", update(7, null, 9));
        check("a parameters-only update of the caller's own stream is accepted", f.succeeded());
        check("the guard checks the STORED statement, not a null", guarded.equals(List.of(STORED)));
        QueryArgument ran = executed.isEmpty() ? null : executed.get(0);
        check("the provider receives the complete statement", ran != null && STORED.equals(ran.getStatement()));
        check("with the caller's own value attached — the hole a null-statement skip would have opened",
            ran != null && Arrays.asList(ran.getParameterNames() == null ? new String[0] : ran.getParameterNames()).contains("caller.person")
                && Arrays.asList(ran.getParameters()).contains(101));
        check("and the client's new parameter kept", ran != null && Integer.valueOf(9).equals(ran.getParameters()[0]));

        // ── Nothing to complete it with ──
        reset();
        f = call(endpoint, "intruder", update(7, null, 9));
        check("another client's stream gives no statement: refused", !f.succeeded()
            && ClientQueryGuard.UNCHECKABLE_REFUSED.equals(f.cause().getMessage()));
        check("and nothing reached the provider", executed.isEmpty());
        reset();
        f = call(endpoint, "owner", update(null, null, 9));
        check("a null statement opening a stream (no id) is refused", !f.succeeded() && executed.isEmpty());
        check("without the guard ever seeing a null", !guarded.contains(null));

        // ── Unchanged paths ──
        reset();
        f = call(endpoint, "owner", update(7, "select id from Event e", 9));
        check("a full statement is checked as sent", f.succeeded() && guarded.equals(List.of("select id from Event e")));
        reset();
        f = call(endpoint, "owner", QueryPushArgument.builder().setQueryStreamId(7).setActive(false).build());
        check("a control message (no query) passes unchecked", f.succeeded() && guarded.isEmpty() && executedArgs.size() == 1);

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }

    static void reset() { guarded.clear(); executed.clear(); executedArgs.clear(); }

    static QueryPushArgument update(Integer queryStreamId, String statement, Object parameter) {
        QueryArgument qa = QueryArgument.builder().setDataSourceId("ds").setLanguage("DQL")
            .setStatement(statement).setParameters(parameter).build();
        return QueryPushArgument.builder().setQueryStreamId(queryStreamId).setQueryArgument(qa).setActive(true).build();
    }

    static Future<Object> call(ExecuteQueryPushMethodEndpoint endpoint, String runId, QueryPushArgument arg) {
        Object state = StateAccessor.setRunId(StateAccessor.createEmptyState(), runId);
        return ThreadLocalStateHolder.runWithState(state, () -> endpoint.toAsyncFunction().apply(arg));
    }
}
