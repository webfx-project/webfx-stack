package dev.webfx.stack.push;

/**
 * @author Bruno Salmon
 */
public final class ClientPushBusAddressesSharedByBothClientAndServer {

    public final static String PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS = "service/push/client/ping";

    public static String computeClientBusCallServiceAddress(Object clientRunId) {
        return "busCallService/client/" + clientRunId;
    }
}
