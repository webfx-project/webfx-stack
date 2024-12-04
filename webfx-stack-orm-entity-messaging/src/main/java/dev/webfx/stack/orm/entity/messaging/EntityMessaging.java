package dev.webfx.stack.orm.entity.messaging;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.binding.EntityBindings;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityResult;
import dev.webfx.stack.orm.entity.result.impl.EntityResultImpl;
import dev.webfx.stack.session.state.client.fx.FXConnected;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class EntityMessaging {

    private final String address;
    private List<Handler<Object>> messageHandlers;
    private Registration registration;

    public EntityMessaging(String address) {
        this.address = address;
    }

    // This is the method to use for the sender client (typically the back-office) to send a message to all front-office
    // clients currently connected and listening (because they called addMessageBodyHandler() before).
    public void publishMessage(Object messageBody) {
        // We serialize the java object into json before publishing it on the event bus
        Object encodedBody = SerialCodecManager.encodeToJson(messageBody);
        // We publish the json encoded body to all front-office clients who are listening (because they called
        // addFrontOfficeMessageBodyHandler() before).
        BusService.bus().publish(address, encodedBody);
    }

    // This is the method to use for the front-office client to listen messages published by the back-office.
    public Registration addMessageBodyHandler(Handler<Object> messageBodyHandler) {
        if (messageHandlers == null) { // Initialization on first call
            messageHandlers = new ArrayList<>();
            // To make this work, the client bus call service must listen server calls! This takes place as soon as the
            // connection to the server is ready, or each time we reconnect to the server:
            FXConnected.runOnEachConnected(() ->
                BusService.bus().register(address, message -> {
                    // Since the message body is encoded into json, we decode it into a java object
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

    public void publishEntityChanges(EntityChanges entityChanges) {
        publishEntityChanges(entityChanges.getInsertedUpdatedEntityResult());
    }

    public void publishEntityChanges(EntityResult entityChanges) {
        if (!entityChanges.getEntityIds().isEmpty())
            publishMessage(entityChanges);
    }

    public void listenEntityChanges(EntityStore entityStore) {
        EntityBindings.registerStoreForEntityChanges(entityStore);
        if (registration == null) {
            registration = addMessageBodyHandler(EntityBindings::applyEntityChangesToRegisteredStores, EntityResultImpl.class);
        }
    }
}
