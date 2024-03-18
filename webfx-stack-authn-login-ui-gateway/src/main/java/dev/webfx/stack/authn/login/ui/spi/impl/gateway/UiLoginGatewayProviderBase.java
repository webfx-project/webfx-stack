package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

/**
 * @author Bruno Salmon
 */
public abstract class UiLoginGatewayProviderBase implements UiLoginGatewayProvider {

    private final Object gatewayId;

    public UiLoginGatewayProviderBase(Object gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Override
    public Object getGatewayId() {
        return gatewayId;
    }


}
