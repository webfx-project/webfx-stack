package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.call.BusCallModuleBooter;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.session.state.client.ClientSideStateSession;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Boots a headless WebFX client, connects its bus, and refuses to hand anything back until the process is in
 * a state where a probe means what it says.
 *
 * <h3>It does not boot, and that is the point of where it sits</h3>
 *
 * <p>The platform has already booted by the time any command runs: {@code ApplicationBooter} is the entry
 * point, the configuration is loaded, and the application's own variables resolver stands ready to supply the
 * server. So this class does not start anything — it checks that what started is what was asked for, and
 * waits.
 *
 * <p><b>The configuration substitutes its variables on READ, not at a boot level.</b> {@code ConfigImpl}
 * calls {@code Substitutor.substitute} in its getter, per value, so {@code BUS_SERVER_HOST} and its two
 * companions are resolved at whatever moment something first asks for them — which is when the bus is created.
 * In an ordinary WebFX client that is {@code COMMUNICATION_REGISTER_BOOT_LEVEL} (5), during the boot; here the
 * endpoint registration is deferred for a command-line program, so it is {@link #connect} below, after the
 * boot. An earlier version of this comment claimed substitution happened once at {@code CONF_BOOT_LEVEL} (3),
 * and that claim cost an afternoon: {@code signout} deletes its session cache before connecting, and so
 * deleted the answer the resolver had not yet been asked for.
 *
 * <h3>The order here is the design</h3>
 *
 * <ol>
 *   <li><b>Read the configuration back.</b> Every {@code BUS_SERVER_*} variable falls back to
 *       {@code localhost:80} when it does not resolve, so a missing substitution plugin, a renamed variable or
 *       a resolver that did not answer does not fail — it connects somewhere else. Comparing what the
 *       configuration says against what the command asked for is the only thing that turns that into an error
 *       rather than a run against the wrong server. It is also what proves the resolver worked.</li>
 *   <li><b>Check the client path.</b> See {@link ClientPath}: a probe that does not travel the bus endpoint
 *       tests none of the controls this tool exists to measure, and would say so with a number.</li>
 *   <li><b>Then wait for the connection</b>, with a deadline.</li>
 * </ol>
 *
 * <p>The first two come before the wait deliberately. Both are about what this process IS rather than about
 * what the server does, so they can be answered without the network — and a tool that would have measured the
 * wrong thing should say so before it signs anybody in.
 *
 * @author Claude Code
 */
public final class ClientRuntime implements AutoCloseable {

    private final ServerEndpoint endpoint;
    private final Object dataSourceId;
    private final Bus bus;

    private ClientRuntime(ServerEndpoint endpoint, Object dataSourceId, Bus bus) {
        this.endpoint = endpoint;
        this.dataSourceId = dataSourceId;
        this.bus = bus;
    }

    /**
     * Checks that the already-booted platform is pointed where the command asked, and waits for the bus.
     *
     * @throws IllegalStateException when the process is not in a state where a probe is meaningful; the
     *                               message says which of the three steps above failed and what to do
     */
    public static ClientRuntime connect(ServerEndpoint endpoint, Object dataSourceId, long connectTimeoutMillis) {
        String configurationProblem = configurationProblem(endpoint);
        if (configurationProblem != null)
            throw new IllegalStateException(configurationProblem);

        // A command-line program's bus-call endpoints are deliberately NOT registered during the boot, because
        // registering one instantiates the bus and instantiating the bus opens the connection — which would have every
        // command that never reaches the server, this tool's help and catalogue included, dial whatever host the
        // configuration defaults to. This is the point at which a command has turned out to need the server, so the
        // endpoints go in now; the call does nothing in an application, or if something already triggered it.
        BusCallModuleBooter.registerDeferredEndpoints();

        Bus bus = BusService.bus();
        // The DECLARED provider, not QueryService.getProvider(): by now the DQL interceptor has wrapped the
        // latter in a lambda. See ClientPath.declaredQueryServiceProvider.
        String pathProblem = ClientPath.refusal(dataSourceId, bus, ClientPath.declaredQueryServiceProvider());
        if (pathProblem != null)
            throw new IllegalStateException("This build cannot issue a meaningful probe: " + pathProblem);

        awaitOpen(bus, connectTimeoutMillis, endpoint);
        listenForServerPushes();
        return new ClientRuntime(endpoint, dataSourceId, bus);
    }

    public ServerEndpoint endpoint() {
        return endpoint;
    }

    public Object dataSourceId() {
        return dataSourceId;
    }

    public Bus bus() {
        return bus;
    }

    /**
     * Blocks on a WebFX future, which is what a one-shot command-line tool wants and an app never does.
     *
     * <p>Safe here because the callbacks run on the JRE scheduler's own threads, never on this one: a CLI's
     * main thread has nothing to do but wait. The timeout is not optional — a bus call that is never answered
     * (a server that accepted the socket and then said nothing) would otherwise hang a probe for ever, and a
     * hung probe in a rehearsal reads as "still running" rather than as a failure.
     */
    public static <T> T await(Future<T> future, long timeoutMillis, String what) {
        CompletableFuture<AsyncResult<T>> bridge = new CompletableFuture<>();
        future.onComplete(bridge::complete);
        AsyncResult<T> result;
        try {
            result = bridge.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException(what + " did not answer within " + timeoutMillis + "ms.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(what + " was interrupted.");
        } catch (ExecutionException e) {
            throw new IllegalStateException(what + " failed: " + e.getCause(), e.getCause());
        }
        if (result.failed()) {
            Throwable cause = result.cause();
            // A refused bus call comes back as a message, not as an exception with a stack: the refusals this
            // tool is here to observe ("A client query must be DQL", "The caller's permissions are not ready
            // yet") are strings. Surfacing the message verbatim is the point.
            throw new BusCallFailure(what + " was refused: " + (cause == null ? "(no reason given)" : cause.getMessage()), cause);
        }
        return result.result();
    }

    /** A call the server answered with a failure, as opposed to one that never came back. */
    public static final class BusCallFailure extends RuntimeException {
        public BusCallFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public void close() {
        try {
            if (bus != null && bus.isOpen())
                bus.close();
        } catch (RuntimeException e) {
            // Closing a socket we are finished with: nothing a run's result depends on.
        }
    }

    /**
     * Registers the two addresses a server reaches this client on — which is a precondition for signing in at
     * all, not a convenience.
     *
     * <h3>The session arrives by push, not by reply</h3>
     *
     * <p>{@code ModalityPasswordAuthenticationGateway.mintAndPush} mints the authenticated state and then
     * composes {@code PushServerService.pushState(state, runId)}, mapping the reply to {@code null}. So the
     * authenticate call answers with nothing and the token is delivered to
     * {@code busCallService/client/<runId>}; renewal and logout reach a client the same way. A client not
     * listening there fails its sign-in with {@code Server error: No handlers for address
     * busCallService/client/…} <i>after</i> the password has been accepted and the session minted — the
     * server is reporting that it could not hand over what it had just created.
     *
     * <h3>Why this is done here rather than by the push client module</h3>
     *
     * <p>{@code PushClientService} does exactly these two registrations, from a static initializer, hooked to
     * {@code FXConnected}. Depending on it was tried and reverted: {@code webfx-stack-push-client-simple}
     * resolves {@code webfx-stack-db-querypush-client-simple} with it, whose {@code SimpleQueryPushClientJob}
     * touches {@code PushClientService} during the BOOT — and registering a bus-call endpoint instantiates the
     * bus, so every command that never reaches a server, {@code help} and {@code status} included, would dial
     * whatever host the configuration defaults to. That is the same connection {@code BusCallModuleBooter}
     * defers for a CLI, arriving by another door. It also brought {@code javafx.controls}, the Modality client
     * entity, i18n and time modules and two visual registry SPIs into a terminal program.
     *
     * <p>So the registrations are made here, at the one moment a command has turned out to need a server, from
     * the addresses' own shared constants rather than from a copy of their format.
     *
     * <p><b>Once, for the current run id.</b> A browser client re-registers on every reconnect because it
     * lives for hours; this process signs in once and exits. A run id that changed mid-run would leave the
     * client listening at the previous address, which is worth knowing and is not worth a listener here.
     */
    private static void listenForServerPushes() {
        String runId = ClientSideStateSession.getInstance().getRunId();
        if (runId == null)
            throw new IllegalStateException(
                "This client has no run id, so the server would have no address to deliver a session to."
                + " Signing in would fail with 'No handlers for address busCallService/client/null' after the"
                + " password had already been accepted. Refusing before that happens.");
        // The ping endpoint, as PushClientService registers it — but SILENT, unlike that one.
        //
        // It was logged at first, on the reasoning that a tool whose subject is what the server does to a
        // caller should say when the server calls it. That is right in general and wrong here: a keep-alive
        // ping is the only thing this address receives, so the line carried no information and arrived twice
        // per invocation — 44 lines across a 22-probe sweep. PushClientService logs it because a browser
        // client connects once and lives for hours; a one-shot command connects, asks one question and exits.
        //
        // Anything OTHER than a ping would arrive at the run's own address below, not here, and that one is a
        // bus entry point whose calls are dispatched to real endpoints rather than swallowed.
        Function<Object, Object> pingListener = argument -> null;
        BusCallService.registerBusCallEndpoint(
            ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS,
            pingListener);
        // And the client's own address, which is what the server's push is actually addressed to.
        BusCallService.listenBusEntryCalls(
            ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(runId));
    }

    /**
     * Why the loaded configuration does not describe the endpoint that was asked for, or null when it does.
     */
    private static String configurationProblem(ServerEndpoint endpoint) {
        Config root = ConfigLoader.getRootConfig();
        if (root == null)
            return "No configuration was loaded, so the bus has no server to connect to."
                   + " This build is missing the configuration plugin.";
        Config websocket = root.childConfigAt("webfx.stack.com.client.websocket");
        if (websocket == null)
            return "The configuration has no webfx.stack.com.client.websocket block, so the bus would connect"
                   + " to its built-in default (localhost:80). This build is missing"
                   + " webfx-stack-com-bus-json-client-websocket, or webfx update has not been run for it.";
        String host = websocket.getString("serverHost");
        String port = websocket.getString("serverPort");
        Boolean ssl = websocket.getBoolean("serverSSL");
        if (endpoint.matchesConfiguration(host, port, ssl))
            return null;
        return "The bus configuration resolved to " + (Boolean.TRUE.equals(ssl) ? "wss://" : "ws://") + host + ":" + port
               + " but " + endpoint.describe() + " was asked for. The " + ServerEndpoint.HOST_VARIABLE + " /"
               + " " + ServerEndpoint.PORT_VARIABLE + " / " + ServerEndpoint.SSL_VARIABLE + " variables did not"
               + " reach the configuration — most likely the system-property substitution plugin is missing"
               + " from this build. Refusing rather than probing a server nobody asked about.";
    }

    private static void awaitOpen(Bus bus, long timeoutMillis, ServerEndpoint endpoint) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!bus.isOpen()) {
            if (System.currentTimeMillis() >= deadline)
                throw new IllegalStateException("The bus did not connect to " + endpoint.describe() + " within "
                                                + timeoutMillis + "ms.");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while connecting to " + endpoint.describe() + ".");
            }
        }
    }
}
