package dev.webfx.stack.mail.transport;

/**
 * What a transport provider actually supports, so application features (suppression sync,
 * bounce flagging…) can adapt instead of failing.
 *
 * @author Bruno Salmon
 */
public final class MailTransportCapabilities {

    private final boolean attachments;
    private final boolean suppressionList; // listSuppressed / removeSuppression are functional
    private final boolean deliveryEvents;  // addFeedbackListener delivers real events

    public MailTransportCapabilities(boolean attachments, boolean suppressionList, boolean deliveryEvents) {
        this.attachments = attachments;
        this.suppressionList = suppressionList;
        this.deliveryEvents = deliveryEvents;
    }

    public boolean supportsAttachments() { return attachments; }
    public boolean supportsSuppressionList() { return suppressionList; }
    public boolean supportsDeliveryEvents() { return deliveryEvents; }
}
