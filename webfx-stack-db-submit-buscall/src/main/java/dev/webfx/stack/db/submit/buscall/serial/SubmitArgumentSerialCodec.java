package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.WriteIntent;

public final class SubmitArgumentSerialCodec extends SerialCodecBase<SubmitArgument> {

    private static final String CODEC_ID = "SubmitArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String DATA_SCOPE_KEY = "dataScope";
    private static final String RETURN_GENERATED_KEYS_KEY = "genKeys";
    private static final String LANGUAGE_KEY = "lang";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";
    private static final String PRIORITY_KEY = "priority";
    private static final String TRANSACTION_PREAMBLE_KEY = "preamble";
    // The declared intent, flattened rather than nested: four scalars cost no codec of their own, and a
    // nested object would need one registered in META-INF/services and webfx.xml to decode at all.
    private static final String INTENT_CODE_KEY = "intentCode";
    private static final String INTENT_ORGANIZATION_KEY = "intentOrg";
    private static final String INTENT_EVENT_KEY = "intentEvent";
    private static final String INTENT_GLOBAL_KEY = "intentGlobal";

    public SubmitArgumentSerialCodec() {
        super(SubmitArgument.class, CODEC_ID);
    }

    @Override
    public void encode(SubmitArgument arg, AstObject serial) {
        encodeObject(        serial, DATA_SOURCE_ID_KEY,        arg.getDataSourceId());
        encodeObject(        serial, DATA_SCOPE_KEY,            arg.getDataScope());
        encodeBoolean(       serial, RETURN_GENERATED_KEYS_KEY, arg.returnGeneratedKeys());
        encodeString(        serial, LANGUAGE_KEY,              arg.getLanguage());
        encodeString(        serial, STATEMENT_KEY,             arg.getStatement());
        if (!Arrays.isEmpty(arg.getParameters()))
            encodeObjectArray(serial, PARAMETERS_KEY,           arg.getParameters());
        encodeInteger(serial, PRIORITY_KEY, arg.getPriority(), SubmitArgument.STANDARD_PRIORITY);
        // Written only when set, so an ordinary statement's serial form is byte-for-byte what it was
        // before this field existed, and an older peer's message simply decodes to false.
        if (arg.isTransactionPreamble())
            encodeBoolean(serial, TRANSACTION_PREAMBLE_KEY, true);
        // Written only when declared, for the reason above: a write that declares nothing serialises
        // exactly as it did before this field existed, and an older peer's message decodes to null —
        // which is the refusable state, not a permissive one.
        WriteIntent intent = arg.getWriteIntent();
        if (intent != null) {
            encodeString(serial, INTENT_CODE_KEY, intent.getOperationCode());
            encodeObject(serial, INTENT_ORGANIZATION_KEY, intent.getOrganizationId());
            encodeObject(serial, INTENT_EVENT_KEY, intent.getEventId());
            // The global flag is written only when true, so an organization-scoped claim adds three
            // keys and not four — and a missing flag decodes to false, which is "not global" rather
            // than "global", the safe direction.
            if (intent.isGlobal())
                encodeBoolean(serial, INTENT_GLOBAL_KEY, true);
        }
    }

    /**
     * The declared intent, or null when the message carries none.
     *
     * <p>Decoded through {@link WriteIntent#decoded} rather than a factory, so a malformed claim — a
     * code with no context, say — arrives as what the peer actually sent. A decoder that repaired it
     * into something well-formed would hide the only thing worth seeing about it.
     */
    private WriteIntent decodeWriteIntent(ReadOnlyAstObject serial) {
        String operationCode = decodeString(serial, INTENT_CODE_KEY);
        if (operationCode == null)
            return null;
        return WriteIntent.decoded(operationCode,
                decodeObject(serial, INTENT_ORGANIZATION_KEY),
                decodeObject(serial, INTENT_EVENT_KEY),
                Boolean.TRUE.equals(decodeBoolean(serial, INTENT_GLOBAL_KEY, false)));
    }

    @Override
    public SubmitArgument decode(ReadOnlyAstObject serial) {
        Integer priority = decodeInteger(serial, PRIORITY_KEY);
        return new SubmitArgument(null,
                decodeObject(     serial, DATA_SOURCE_ID_KEY),
                decodeObject(     serial, DATA_SCOPE_KEY),
                decodeBoolean(    serial, RETURN_GENERATED_KEYS_KEY),
                decodeString(     serial, LANGUAGE_KEY),
                decodeString(     serial, STATEMENT_KEY),
                decodeObjectArray(serial, PARAMETERS_KEY),
                priority == null ? SubmitArgument.STANDARD_PRIORITY : priority,
                Boolean.TRUE.equals(decodeBoolean(serial, TRANSACTION_PREAMBLE_KEY, false)),
                decodeWriteIntent(serial)
        );
    }
}
