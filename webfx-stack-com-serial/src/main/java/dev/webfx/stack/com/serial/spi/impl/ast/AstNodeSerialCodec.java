package dev.webfx.stack.com.serial.spi.impl.ast;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstNode;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * @author Bruno Salmon
 */
public final class AstNodeSerialCodec extends SerialCodecBase<ReadOnlyAstNode> {

    private static final String CODEC_ID = "AstNode";
    private static final String VALUE_KEY = "v";

    public AstNodeSerialCodec() {
        super(ReadOnlyAstNode.class, CODEC_ID);
    }

    @Override
    public void encode(ReadOnlyAstNode value, AstObject serial) {
        serial.set(VALUE_KEY, value);
    }

    @Override
    public ReadOnlyAstNode decode(ReadOnlyAstObject serial) {
        return serial.get(VALUE_KEY);
    }
}
