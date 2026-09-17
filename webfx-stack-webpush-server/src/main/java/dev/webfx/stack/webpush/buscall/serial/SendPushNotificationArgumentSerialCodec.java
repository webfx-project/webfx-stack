package dev.webfx.stack.webpush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.webpush.buscall.SendPushNotificationArgument;

/**
 * Wire codec for {@link SendPushNotificationArgument}.
 * <p>
 * The {@code target} field is encoded as a polymorphic Object — its concrete
 * type is identified by the embedded {@code $codec} discriminator. The host
 * application provides its own SerialCodec for whatever target type it uses
 * (e.g. modality's {@code ModalityWebPushTargetSerialCodec}); the
 * SerialCodecManager dispatches automatically.
 *
 * @author Bruno Salmon
 */
public final class SendPushNotificationArgumentSerialCodec extends SerialCodecBase<SendPushNotificationArgument> {

    private static final String CODEC_ID = "SendPushNotificationArgument";
    private static final String TARGET_KEY = "target";
    private static final String TITLE_KEY = "title";
    private static final String BODY_KEY = "body";
    private static final String URL_KEY = "url";
    private static final String TEST_SEND_TO_SELF_KEY = "testSendToSelf";

    public SendPushNotificationArgumentSerialCodec() {
        super(SendPushNotificationArgument.class, CODEC_ID);
    }

    @Override
    public void encode(SendPushNotificationArgument arg, AstObject serial) {
        encodeObject( serial, TARGET_KEY,           arg.target());
        encodeString( serial, TITLE_KEY,            arg.title());
        encodeString( serial, BODY_KEY,             arg.body());
        encodeString( serial, URL_KEY,              arg.url());
        encodeBoolean(serial, TEST_SEND_TO_SELF_KEY, arg.testSendToSelf());
    }

    @Override
    public SendPushNotificationArgument decode(ReadOnlyAstObject serial) {
        return new SendPushNotificationArgument(
                decodeObject( serial, TARGET_KEY),
                decodeString( serial, TITLE_KEY),
                decodeString( serial, BODY_KEY),
                decodeString( serial, URL_KEY),
                decodeBoolean(serial, TEST_SEND_TO_SELF_KEY)
        );
    }
}
