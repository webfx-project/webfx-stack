package dev.webfx.stack.webpush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.webpush.buscall.UnsubscribePushNotificationsResult;

/**
 * Wire codec for {@link UnsubscribePushNotificationsResult}.
 *
 * @author Bruno Salmon
 */
public final class UnsubscribePushNotificationsResultSerialCodec extends SerialCodecBase<UnsubscribePushNotificationsResult> {

    private static final String CODEC_ID = "UnsubscribePushNotificationsResult";
    private static final String EMAIL_KEY = "email";
    private static final String DISABLED_KEY = "disabled";

    public UnsubscribePushNotificationsResultSerialCodec() {
        super(UnsubscribePushNotificationsResult.class, CODEC_ID);
    }

    @Override
    public void encode(UnsubscribePushNotificationsResult arg, AstObject serial) {
        encodeString( serial, EMAIL_KEY,    arg.email());
        encodeInteger(serial, DISABLED_KEY, arg.disabled());
    }

    @Override
    public UnsubscribePushNotificationsResult decode(ReadOnlyAstObject serial) {
        return new UnsubscribePushNotificationsResult(
                decodeString( serial, EMAIL_KEY),
                decodeInteger(serial, DISABLED_KEY)
        );
    }
}
