package dev.webfx.stack.db.query.cli;

/**
 * The server a run talks to, as the operator names it and as the bus configuration will read it back.
 *
 * <p>A record rather than three loose strings because the CLI has to do two things with it: supply the
 * {@code BUS_SERVER_*} variables that the websocket bus's configuration substitutes — through a
 * {@code VariablesResolver}, since the bus connects during the boot and nothing can be set after it — and then
 * CHECK that the configuration really resolved to what was asked for. The second matters more than it looks:
 * every one of those variables falls back to {@code localhost:80} when unresolved, so a typo in a variable
 * name, a resolver that did not answer, or a substitution plugin left out of the build, produces a run that
 * connects somewhere else entirely — or to nothing — rather than an error.
 *
 * <p>The variable NAMES are here rather than in the resolver because this is the type that knows what they
 * mean; an application supplies them however its platform allows.
 *
 * @author Claude Code
 */
public record ServerEndpoint(String host, int port, boolean ssl) {

    /** The variable names in the {@code webfx.stack.com.client.websocket} configuration block. */
    public static final String HOST_VARIABLE = "BUS_SERVER_HOST";
    public static final String PORT_VARIABLE = "BUS_SERVER_PORT";
    public static final String SSL_VARIABLE = "BUS_SERVER_SSL";

    /**
     * The endpoint this URL names, or null when it names none.
     *
     * <p>Accepts what an operator would paste: {@code wss://host}, {@code ws://host:8080}, and the http forms
     * of both, since the bus endpoint is reached on the app's own origin. The port defaults by scheme, which
     * is the part worth getting right — an https origin with no port is 443, and defaulting it to 80 would
     * connect in clear to a port that is usually closed.
     */
    public static ServerEndpoint parse(String url) {
        if (url == null)
            return null;
        String rest;
        boolean ssl;
        if (url.startsWith("wss://") || url.startsWith("https://")) {
            ssl = true;
            rest = url.substring(url.indexOf("//") + 2);
        } else if (url.startsWith("ws://") || url.startsWith("http://")) {
            ssl = false;
            rest = url.substring(url.indexOf("//") + 2);
        } else {
            return null;
        }
        int slash = rest.indexOf('/');
        if (slash >= 0)
            rest = rest.substring(0, slash);
        if (rest.isEmpty())
            return null;
        int colon = rest.lastIndexOf(':');
        if (colon < 0)
            return new ServerEndpoint(rest, ssl ? 443 : 80, ssl);
        String host = rest.substring(0, colon);
        if (host.isEmpty())
            return null;
        try {
            int port = Integer.parseInt(rest.substring(colon + 1));
            if (port <= 0 || port > 65535)
                return null;
            return new ServerEndpoint(host, port, ssl);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** How this endpoint reads back out of the configuration, for the comparison the runtime makes. */
    public boolean matchesConfiguration(String configuredHost, String configuredPort, Boolean configuredSsl) {
        return host.equals(configuredHost)
               && String.valueOf(port).equals(configuredPort)
               && Boolean.valueOf(ssl).equals(configuredSsl);
    }

    /**
     * The same server as an HTTP origin, which is what an emailed login link has to be built from.
     *
     * <p>{@code SendMagicLinkCredentials} takes a {@code clientOrigin} and its comment says why: "the magic
     * link will start with the same origin, so it goes back to the same server". Derived here rather than
     * asked for a second time — a run that named {@code wss://host} on the command line and then had to
     * repeat {@code https://host} could get the two wrong relative to each other, and the failure would be a
     * link pointing at a server nobody is testing.
     *
     * <p>The port is omitted when it is the scheme's own, because an origin carrying {@code :443} is not
     * wrong but is not what anything else in the system writes.
     */
    public String httpOrigin() {
        boolean defaultPort = port == (ssl ? 443 : 80);
        return (ssl ? "https://" : "http://") + host + (defaultPort ? "" : ":" + port);
    }

    /** The bus URI this endpoint produces, for log lines and refusal messages. */
    public String describe() {
        return (ssl ? "wss://" : "ws://") + host + ":" + port;
    }

    @Override
    public String toString() {
        return describe();
    }
}
