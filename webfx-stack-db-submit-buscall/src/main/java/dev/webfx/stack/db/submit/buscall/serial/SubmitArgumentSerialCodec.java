package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.SubmitArgument;

public final class SubmitArgumentSerialCodec extends SerialCodecBase<SubmitArgument> {

    private static final String CODEC_ID = "SubmitArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String DATA_SCOPE_KEY = "dataScope";
    private static final String RETURN_GENERATED_KEYS_KEY = "genKeys";
    private static final String LANGUAGE_KEY = "lang";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";

    public SubmitArgumentSerialCodec() {
        super(SubmitArgument.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(SubmitArgument arg, AstObject json) {
        json.set(DATA_SOURCE_ID_KEY, arg.getDataSourceId());
        if (arg.getDataScope() != null)
            json.set(DATA_SCOPE_KEY, SerialCodecManager.encodeToJson(arg.getDataScope()));
        json.set(RETURN_GENERATED_KEYS_KEY, arg.returnGeneratedKeys());
        if (arg.getLanguage() != null)
            json.set(LANGUAGE_KEY, arg.getLanguage());
        json.set(STATEMENT_KEY, arg.getStatement());
        if (!Arrays.isEmpty(arg.getParameters()))
            json.set(PARAMETERS_KEY, SerialCodecManager.encodePrimitiveArrayToAstArray(arg.getParameters()));
    }

    @Override
    public SubmitArgument decodeFromJson(ReadOnlyAstObject json) {
        return new SubmitArgument(null,
                json.get(DATA_SOURCE_ID_KEY),
                SerialCodecManager.decodeFromJson(json.getObject(DATA_SCOPE_KEY)),
                json.getBoolean(RETURN_GENERATED_KEYS_KEY),
                json.getString(LANGUAGE_KEY),
                json.getString(STATEMENT_KEY),
                SerialCodecManager.decodePrimitiveArrayFromAstArray(json.getArray(PARAMETERS_KEY))
        );
    }
}
