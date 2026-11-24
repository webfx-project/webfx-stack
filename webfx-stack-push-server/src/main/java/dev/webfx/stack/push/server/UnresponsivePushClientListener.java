package dev.webfx.stack.push.server;

/**
 * @author Bruno Salmon
 */
public interface UnresponsivePushClientListener {

    /** The client stayed unreachable through every probe and is given up — drop what it held. */
    void onUnresponsivePushClient(Object clientRunId);

    /**
     * A push to the client had failed, and it has answered again before being given up. Whatever was
     * pushed to it in between may be lost, so anything it subscribes to should be sent again in full.
     */
    default void onPushClientReachableAgain(Object clientRunId) {
    }

}
