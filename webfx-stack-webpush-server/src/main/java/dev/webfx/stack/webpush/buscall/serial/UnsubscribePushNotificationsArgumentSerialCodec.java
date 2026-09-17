package dev.webfx.stack.webpush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.webpush.buscall.UnsubscribePushNotificationsArgument;

/**
 * Wire codec for {@link UnsubscribePushNotificationsArgument}.
 *
 * @author Bruno Salmon
 */
public final class UnsubscribePushNotificationsArgumentSerialCodec extends SerialCodecBase<UnsubscribePushNotificationsArgument> {

    private static final String CODEC_ID = "UnsubscribePushNotificationsArgument";
    private static final String EMAIL_KEY = "email";

    public UnsubscribePushNotificationsArgumentSerialCodec() {
        super(UnsubscribePushNotificationsArgument.class, CODEC_ID);
    }

    @Override
    public void encode(UnsubscribePushNotificationsArgument arg, AstObject serial) {
        encodeString(serial, EMAIL_KEY, arg.email());
    }

    @Override
    public UnsubscribePushNotificationsArgument decode(ReadOnlyAstObject serial) {
        return new UnsubscribePushNotificationsArgument(
                decodeString(serial, EMAIL_KEY)
        );
    }
}
