package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.ast.AST;
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

    /** Call number of a payload that could not be decoded, or {@link #UNKNOWN_CALL_NUMBER}. */
    static final int UNKNOWN_CALL_NUMBER = -1;

    /**
     * Reads the call number straight out of a raw, not-yet-decoded call payload, without decoding it.
     * <p>
     * Needed to answer a call whose decoding is what failed: the codec below decodes the target
     * argument eagerly, so an argument naming a codec this peer doesn't have takes the whole
     * BusCallArgument down with it, and BusCallService then has to reply to a call it never managed
     * to read. The call number isn't what identifies the call to either client — both key their
     * pending calls on the bus reply address — so this is for the reply's own bookkeeping, and
     * UNKNOWN_CALL_NUMBER is a serviceable answer when even the raw payload won't yield one.
     */
    static int readCallNumberFromRawPayload(Object rawPayload) {
        if (AST.isObject(rawPayload)) {
            Integer callNumber = ((ReadOnlyAstObject) rawPayload).getInteger(ProvidedSerialCodec.CALL_NUMBER_KEY);
            if (callNumber != null)
                return callNumber;
        }
        return UNKNOWN_CALL_NUMBER;
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
