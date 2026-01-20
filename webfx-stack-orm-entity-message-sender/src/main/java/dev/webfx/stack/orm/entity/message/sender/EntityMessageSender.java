package dev.webfx.stack.orm.entity.message.sender;

import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityResult;

/**
 * @author Bruno Salmon
 */
public final class EntityMessageSender {

    private final String address;

    public EntityMessageSender(String address) {
        this.address = address;
    }

    // This is the method to use for the sender client (typically the back-office) to send a message to all front-office
    // clients currently connected and listening (because they called addMessageBodyHandler() before).
    public void publishMessage(Object messageBody) {
        // We serialize the java object into JSON before publishing it on the event bus
        Object encodedBody = SerialCodecManager.encodeToJson(messageBody);
        // We publish the json-encoded body to all front-office clients who are listening to (because they called
        // addFrontOfficeMessageBodyHandler() before).
        BusService.bus().publish(address, encodedBody);
    }

    public void publishEntityChanges(EntityChanges entityChanges) {
        publishEntityChanges(entityChanges.getInsertedUpdatedEntityResult());
    }

    public void publishEntityChanges(EntityResult entityChanges) {
        if (!entityChanges.getEntityIds().isEmpty())
            publishMessage(entityChanges);
    }

}
