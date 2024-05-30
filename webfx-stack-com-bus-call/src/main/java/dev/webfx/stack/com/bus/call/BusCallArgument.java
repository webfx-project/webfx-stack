package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.platform.ast.ReadOnlyAstObject;


/*
 * @author Bruno Salmon
 */
public final class BusCallArgument {

    private static int SEQ = 0;

    private final String targetAddress;
    private final Object targetArgument;
    private final int callNumber;

    private Object jsonEncodedTargetArgument; // can be a AstObject or simply a scalar

    BusCallArgument(String targetAddress, Object targetArgument) {
        this(targetAddress, targetArgument, ++SEQ);
    }

    private BusCallArgument(String targetAddress, Object targetArgument, int callNumber) {
        this.targetAddress = targetAddress;
        this.targetArgument = targetArgument;
        this.callNumber = callNumber;
    }

    String getTargetAddress() {
        return targetAddress;
    }

    Object getTargetArgument() {
        return targetArgument;
    }

    int getCallNumber() {
        return callNumber;
    }

    Object getJsonEncodedTargetArgument() {
        if (jsonEncodedTargetArgument == null && targetArgument != null)
            jsonEncodedTargetArgument = SerialCodecManager.encodeToJson(targetArgument);
        return jsonEncodedTargetArgument;
    }

    /****************************************************
     *                   Serial ProvidedSerialCodec                   *
     * *************************************************/

    public static final class ProvidedSerialCodec extends SerialCodecBase<BusCallArgument> {

        private static final String CODEC_ID = "call";
        private static final String TARGET_ADDRESS_KEY = "addr";
        private static final String TARGET_ARGUMENT_KEY = "arg";
        private static final String CALL_NUMBER_KEY = "seq";

        public ProvidedSerialCodec() {
            super(BusCallArgument.class, CODEC_ID);
        }

        @Override
        public void encode(BusCallArgument call, AstObject serial) {
            encodeString( serial, TARGET_ADDRESS_KEY,  call.getTargetAddress());
            encodeObject( serial, TARGET_ARGUMENT_KEY, call.getJsonEncodedTargetArgument());
            encodeInteger(serial, CALL_NUMBER_KEY,     call.callNumber);
        }

        @Override
        public BusCallArgument decode(ReadOnlyAstObject serial) {
            return new BusCallArgument(
                    decodeString( serial, TARGET_ADDRESS_KEY),
                    decodeObject( serial, TARGET_ARGUMENT_KEY),
                    decodeInteger(serial, CALL_NUMBER_KEY)
            );
        }
    }
}
