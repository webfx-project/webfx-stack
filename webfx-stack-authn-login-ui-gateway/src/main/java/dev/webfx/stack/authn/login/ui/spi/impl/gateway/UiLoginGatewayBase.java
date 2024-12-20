package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

/**
 * @author Bruno Salmon
 */
public abstract class UiLoginGatewayBase implements UiLoginGateway {

    private final Object gatewayId;

    public UiLoginGatewayBase(Object gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Override
    public Object getGatewayId() {
        return gatewayId;
    }


}
