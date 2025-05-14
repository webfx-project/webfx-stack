package dev.webfx.stack.com.bus.spi.impl.json;

public interface JsonBusConstants {

    // Conventional json bus constants
    String BODY = "body";
    String HEADERS = "headers";
    String ADDRESS = "address";
    String REPLY_ADDRESS = "replyAddress";
    String TYPE = "type";
    String SEND = "send";
    String PUBLISH = "publish";
    String REGISTER = "register";
    String UNREGISTER = "unregister";
    String PING = "ping";

    // Constants specific to WebFX for state management
    String HEADERS_STATE = "state"; // Will optionally hold the state of the client. This applies either for incoming
    // message where the client communicates its state to the server (ex: runId, sessionId, etc.), and this state may
    // be enriched when forwarded to an internal server endpoint (so the endpoint can get information about the client)
    // or for outgoing private message sent to a specific client. In the later case, this provides a way for the server
    // to change the state of the client (ex: userId => login, logout).
    String HEADERS_UNICAST = "unicast"; // Internal server header used to flag outgoing messages as private (indicating
    // they will be sent to a specific targeted client). Only these unicast messages should be automatically enriched
    // with the client state.
    String PING_STATE_ADDRESS = "pingState"; // This address is used by any client that just wants to communicate a
    // change in its state to the server.
}
