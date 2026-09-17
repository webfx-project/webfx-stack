package dev.webfx.stack.webpush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.webpush.buscall.SendPushNotificationResult;

/**
 * Wire codec for {@link SendPushNotificationResult}.
 *
 * @author Bruno Salmon
 */
public final class SendPushNotificationResultSerialCodec extends SerialCodecBase<SendPushNotificationResult> {

    private static final String CODEC_ID = "SendPushNotificationResult";
    private static final String TARGETED_KEY  = "targeted";
    private static final String SUCCEEDED_KEY = "succeeded";
    private static final String EXPIRED_KEY   = "expired";
    private static final String FAILED_KEY    = "failed";

    public SendPushNotificationResultSerialCodec() {
        super(SendPushNotificationResult.class, CODEC_ID);
    }

    @Override
    public void encode(SendPushNotificationResult arg, AstObject serial) {
        encodeInteger(serial, TARGETED_KEY,  arg.targeted());
        encodeInteger(serial, SUCCEEDED_KEY, arg.succeeded());
        encodeInteger(serial, EXPIRED_KEY,   arg.expired());
        encodeInteger(serial, FAILED_KEY,    arg.failed());
    }

    @Override
    public SendPushNotificationResult decode(ReadOnlyAstObject serial) {
        return new SendPushNotificationResult(
                decodeInteger(serial, TARGETED_KEY),
                decodeInteger(serial, SUCCEEDED_KEY),
                decodeInteger(serial, EXPIRED_KEY),
                decodeInteger(serial, FAILED_KEY)
        );
    }
}
