package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.call.BusCallModuleBooter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Boots a headless WebFX client, connects its bus, and refuses to hand anything back until the process is in
 * a state where a probe means what it says.
 *
 * <h3>It does not boot, and that is the point of where it sits</h3>
 *
 * <p>The platform has already booted by the time any command runs: {@code ApplicationBooter} is the entry
 * point, the configuration was substituted at {@code CONF_BOOT_LEVEL} (3) with the server supplied by the
 * application's own variables resolver, and the bus began connecting at
 * {@code COMMUNICATION_REGISTER_BOOT_LEVEL} (5). So this class does not start anything — it checks that what
 * started is what was asked for, and waits.
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
