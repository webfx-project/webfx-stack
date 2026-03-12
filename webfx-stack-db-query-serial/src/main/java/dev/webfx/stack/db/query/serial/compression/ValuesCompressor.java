package dev.webfx.stack.db.query.serial.compression;

/**
 * @author Bruno Salmon
 */
public interface ValuesCompressor {

    Object[] compress(Object[] values);

    Object[] uncompress(Object[] compressedValues);

}
