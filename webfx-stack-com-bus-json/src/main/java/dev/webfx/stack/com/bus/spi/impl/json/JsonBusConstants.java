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
    String HEADERS_STATE = "state";
    String PING_STATE_ADDRESS = "pingState";
}
