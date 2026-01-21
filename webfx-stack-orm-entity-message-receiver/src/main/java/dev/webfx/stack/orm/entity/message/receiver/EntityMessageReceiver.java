package dev.webfx.stack.orm.entity.message.receiver;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.binding.EntityBindings;
import dev.webfx.stack.orm.entity.result.impl.EntityResultImpl;
import dev.webfx.stack.session.state.client.fx.FXConnected;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class EntityMessageReceiver {

    private final String address;
    private List<Handler<Object>> messageHandlers;
    private Registration registration;

    public EntityMessageReceiver(String address) {
        this.address = address;
    }

    // This is the method to use for the front-office client to listen to the messages published by the back-office.
    public Registration addMessageBodyHandler(Handler<Object> messageBodyHandler) {
        if (messageHandlers == null) { // Initialization on the first call
            messageHandlers = new ArrayList<>();
            // To make this work, the client bus call service must listen to the server calls! This takes place as soon
            // as the connection to the server is ready, or each time we reconnect to the server:
            FXConnected.runOnEachConnected(() ->
                BusService.bus().register(address, message -> {
                    // Since the message body is encoded into JSON, we decode it into a java object
                    Object messageBody = SerialCodecManager.decodeFromJson(message.body());
                    // Then we call each handler with that java message body
                    messageHandlers.forEach(h -> h.handle(messageBody));
                })
            );
        }
        messageHandlers.add(messageBodyHandler);
        return () -> messageHandlers.remove(messageBodyHandler);
    }

    public <T> Registration addMessageBodyHandler(Handler<T> messageBodyHandler, Class<T> messageClass) {
        return addMessageBodyHandler(messageBody -> {
            if (Objects.isInstanceOf(messageBody, messageClass)) {
                messageBodyHandler.handle((T) messageBody);
            }
        });
    }

    public void listenEntityChanges(EntityStore entityStore) {
        EntityBindings.registerStoreForEntityChanges(entityStore);
        if (registration == null) {
            registration = addMessageBodyHandler(EntityBindings::applyEntityChangesToRegisteredStores, EntityResultImpl.class);
        }
    }
}
