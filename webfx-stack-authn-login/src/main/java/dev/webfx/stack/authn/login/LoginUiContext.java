package dev.webfx.stack.authn.login;

/**
 * @author Bruno Salmon
 */
public class LoginUiContext {

    private final Object gatewayId;
    private final Object gatewayContext;

    public LoginUiContext(Object gatewayId, Object gatewayContext) {
        this.gatewayId = gatewayId;
        this.gatewayContext = gatewayContext;
    }

    public Object getGatewayId() {
        return gatewayId;
    }

    public Object getGatewayContext() {
        return gatewayContext;
    }
}
