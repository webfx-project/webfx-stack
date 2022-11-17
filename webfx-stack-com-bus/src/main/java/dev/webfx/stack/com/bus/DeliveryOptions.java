package dev.webfx.stack.com.bus;

/**
 * @author Bruno Salmon
 */
public final class DeliveryOptions {

    public static final DeliveryOptions LOCAL_ONLY = new DeliveryOptions().setLocalOnly(true);

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
