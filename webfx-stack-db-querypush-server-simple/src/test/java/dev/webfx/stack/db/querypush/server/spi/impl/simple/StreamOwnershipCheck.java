package dev.webfx.stack.db.querypush.server.spi.impl.simple;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.push.server.spi.PushServerServiceProvider;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * That only the client which opened a push stream can read back its statement, change it, or close it.
 *
 * <p>Stream ids are one server-wide sequence. Before this, any client could update or close any stream by
 * counting ids. The results kept going to the owner, so nothing was read this way, but the owner's screen
 * showed whatever the other client asked for. The statement lookup has the same rule, because the
 * executeQueryPush endpoint uses it to complete a parameters-only update.
 *
 * <p>No test framework: runs from main() and exits non-zero on failure.
 */
public class StreamOwnershipCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static final String STATEMENT = "select id from Event e where id=$1";

    public static void main(String[] args) {
        // The provider's constructor registers with the push server; nothing else of it is used here.
        SingleServiceProvider.registerServiceProvider(PushServerServiceProvider.class, (PushServerServiceProvider)
            Proxy.newProxyInstance(PushServerServiceProvider.class.getClassLoader(),
                new Class<?>[]{PushServerServiceProvider.class}, (proxy, method, a) -> null));
        SimpleInMemoryServerQueryPushServiceProvider provider = new SimpleInMemoryServerQueryPushServiceProvider();

        System.out.println("StreamOwnershipCheck");

        // Opened inactive, so no pulse runs (there is no database here).
        QueryArgument qa = QueryArgument.builder().setDataSourceId("ds").setLanguage("DQL")
            .setStatement(STATEMENT).setParameters(1).build();
        Future<Object> opened = as("owner", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryArgument(qa).setActive(false).build()));
        Object id = opened.result();
        check("the owner opens a stream", opened.succeeded() && id != null);

        check("the owner reads back its statement", STATEMENT.equals(as("owner", () -> provider.getStreamStatement(id))));
        check("another client does not", as("intruder", () -> provider.getStreamStatement(id)) == null);
        check("nor a caller with no run id", as(null, () -> provider.getStreamStatement(id)) == null);

        QueryArgument other = QueryArgument.builder().copy(qa).setParameters(2).build();
        Future<Object> hijack = as("intruder", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryStreamId(id).setQueryArgument(other).build()));
        check("another client cannot change the stream's query", !hijack.succeeded());
        check("and the refusal reads as an unknown stream, saying nothing about which ids are open",
            hijack.cause() != null && hijack.cause().getMessage().contains("unregistered"));

        as("intruder", () -> provider.executeQueryPush(QueryPushArgument.builder().setQueryStreamId(id).setClose(true).build()));
        check("another client's close leaves the stream open",
            STATEMENT.equals(as("owner", () -> provider.getStreamStatement(id))));

        // The parent stream. A client names its parent from its OWN active calls
        // (ReactiveQueryPushCall.getActiveParent), so a foreign parent is never legitimate — and adopting
        // one is not harmless: updateActiveStreamCount cascades from a parent to its children, and a
        // QueryInfo is shared by every client on the same argument, so a child hung off a stranger's stream
        // perturbs the active count of a query other clients depend on for their refreshes.
        Future<Object> intruderChild = as("intruder", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryArgument(qa).setActive(false).setParentQueryStreamId(id).build()));
        check("another client may still open its own stream", intruderChild.succeeded());
        check("but it is not adopted as a child of the owner's",
            provider.getStreamInfo(id).childrenStreamInfos.isEmpty());
        as("intruder", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryStreamId(intruderChild.result()).setClose(true).build()));

        // The owner's own child IS adopted — the check must not cost the feature it is protecting.
        Future<Object> ownChild = as("owner", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryArgument(qa).setActive(false).setParentQueryStreamId(id).build()));
        check("the owner's own child is adopted",
            ownChild.succeeded() && provider.getStreamInfo(id).childrenStreamInfos.size() == 1);
        as("owner", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryStreamId(ownChild.result()).setClose(true).build()));

        Future<Object> update = as("owner", () -> provider.executeQueryPush(
            QueryPushArgument.builder().setQueryStreamId(id).setQueryArgument(other).build()));
        check("the owner can still change it", update.succeeded());
        as("owner", () -> provider.executeQueryPush(QueryPushArgument.builder().setQueryStreamId(id).setClose(true).build()));
        check("and close it", as("owner", () -> provider.getStreamStatement(id)) == null);

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }

    static <T> T as(String runId, Supplier<T> call) {
        Object state = StateAccessor.createEmptyState();
        if (runId != null)
            state = StateAccessor.setRunId(state, runId);
        return ThreadLocalStateHolder.runWithState(state, call);
    }
}
