package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.NameCountInfo;

public final class NameCountInfoSerialCodec extends SerialCodecBase<NameCountInfo> {

    private static final String CODEC_ID = "NameCountInfo";
    private static final String NAME_KEY = "name";
    private static final String COUNT_KEY = "count";

    public NameCountInfoSerialCodec() {
        super(NameCountInfo.class, CODEC_ID);
    }

    @Override
    public void encode(NameCountInfo arg, AstObject serial) {
        encodeString( serial, NAME_KEY,  arg.getName());
        encodeInteger(serial, COUNT_KEY, arg.getCount());
    }

    @Override
    public NameCountInfo decode(ReadOnlyAstObject serial) {
        return new NameCountInfo(
            decodeString(serial, NAME_KEY),
            decodeInteger(serial, COUNT_KEY, 0));
    }
}
