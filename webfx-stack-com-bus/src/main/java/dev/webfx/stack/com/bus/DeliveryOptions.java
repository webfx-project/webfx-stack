package dev.webfx.stack.com.bus;

/**
 * @author Bruno Salmon
 */
public final class DeliveryOptions {

    public static DeliveryOptions localOnlyDeliveryOptions() {
        return localOnlyDeliveryOptions(null);
    }

    public static DeliveryOptions localOnlyDeliveryOptions(Object state) {
        return new DeliveryOptions().setLocalOnly(true).setState(state);
    }

    private boolean localOnly;

    private Object state;


    public boolean isLocalOnly() {
        return localOnly;
    }

    public DeliveryOptions setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
        return this;
    }

    public Object getState() {
        return state;
    }

    public DeliveryOptions setState(Object state) {
        this.state = state;
        return this;
    }
}
