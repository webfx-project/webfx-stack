package dev.webfx.stack.com.bus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Who may publish to, and who may receive from, an address the network bridge carries between clients.
 *
 * <p><b>Why it exists.</b> The bridge permits every address in both directions (VertxBusModuleBooter), so any
 * connected client — signed in or not — can listen on an address and publish to it. That is right for
 * request/response calls, whose endpoints decide for themselves, and wrong for client-to-client broadcasts, where
 * nobody decides. An application registers its broadcast addresses here at boot, and the bridge consults this for
 * every message on them. Addresses not registered are untouched.
 *
 * <p><b>Three kinds of rule</b>, each stated in terms of the session's VERIFIED back-office audience (from a
 * token whose signature held), never the client's claim:
 * <ul>
 *   <li>{@link #restrictPublishToVerifiedBackoffice} — a broadcast the back office sends to everyone;</li>
 *   <li>{@link #restrictPublishToServer} — only the server publishes; any client publish is refused;</li>
 *   <li>{@link #restrictDeliveryToVerifiedBackoffice} — only verified back-office sessions receive it.</li>
 * </ul>
 *
 * <p><b>Observe first.</b> A publish rule can be registered observing: the decision is logged by the bridge and the
 * message is still delivered. Server-only and delivery rules are meant for addresses that are new, where nothing
 * existing can break, so they are always enforced.
 *
 * <p>Registered once, at boot, before the bridge accepts connections; read-only afterwards. Plain collections,
 * because this module is also compiled for the browser, where no concurrent collection exists.
 *
 * @author Claude Code
 */
public final class BridgeAddressPolicy {

    /** Address -> whether its back-office-only publish rule is enforced (true) or only observed (false). */
    private static final Map<String, Boolean> BACKOFFICE_PUBLISH = new HashMap<>();
    private static final Set<String> SERVER_PUBLISH = new HashSet<>();
    private static final Set<String> BACKOFFICE_DELIVERY = new HashSet<>();

    /** What the bridge should do with a client's publish. */
    public enum PublishDecision { ALLOW, REFUSE, WOULD_REFUSE }

    private BridgeAddressPolicy() {
    }

    public static void restrictPublishToVerifiedBackoffice(String address, boolean enforcing) {
        BACKOFFICE_PUBLISH.put(address, enforcing);
    }

    public static void restrictPublishToServer(String address) {
        SERVER_PUBLISH.add(address);
    }

    public static void restrictDeliveryToVerifiedBackoffice(String address) {
        BACKOFFICE_DELIVERY.add(address);
    }

    /** Whether any rule names this address — the bridge's fast path skips everything else. */
    public static boolean governs(String address) {
        return address != null
               && (BACKOFFICE_PUBLISH.containsKey(address) || SERVER_PUBLISH.contains(address) || BACKOFFICE_DELIVERY.contains(address));
    }

    /**
     * The decision for a publish (or send) a CLIENT makes to this address. The server's own publishes never pass
     * the bridge and are not judged here.
     */
    public static PublishDecision publishDecision(String address, boolean verifiedBackoffice) {
        if (address == null)
            return PublishDecision.ALLOW;
        if (SERVER_PUBLISH.contains(address))
            return PublishDecision.REFUSE;
        Boolean enforcing = BACKOFFICE_PUBLISH.get(address);
        if (enforcing == null || verifiedBackoffice)
            return PublishDecision.ALLOW;
        return enforcing ? PublishDecision.REFUSE : PublishDecision.WOULD_REFUSE;
    }

    /** Whether a message on this address may be delivered to a client session. */
    public static boolean mayDeliver(String address, boolean verifiedBackoffice) {
        return address == null || verifiedBackoffice || !BACKOFFICE_DELIVERY.contains(address);
    }
}
