package dev.webfx.stack.authn.login.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.login.LoginUiContext;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

public final class LoginUiContextSerialCodec extends SerialCodecBase<LoginUiContext> {

    private final static String CODEC_ID = "LoginUiContext";
    private static final String GATEWAY_ID_KEY = "gatewayId";
    private static final String GATEWAY_CONTEXT_KEY = "gatewayContext";

    public LoginUiContextSerialCodec() {
        super(LoginUiContext.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(LoginUiContext loginUiContext, AstObject json) {
        json
                .set(GATEWAY_ID_KEY, loginUiContext.getGatewayId())
                .set(GATEWAY_CONTEXT_KEY, loginUiContext.getGatewayContext());
    }

    @Override
    public LoginUiContext decodeFromJson(ReadOnlyAstObject json) {
        return new LoginUiContext(
                json.get(GATEWAY_ID_KEY),
                json.get(GATEWAY_CONTEXT_KEY)
        );
    }
}
