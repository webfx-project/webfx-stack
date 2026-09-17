package dev.webfx.stack.db.query.serial.compression.repeat;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.db.query.serial.compression.ValuesCompressor;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public final class RepeatedValuesCompressor implements ValuesCompressor {

    public final static RepeatedValuesCompressor SINGLETON = new RepeatedValuesCompressor();

    private RepeatedValuesCompressor() {}

    @Override
    public Object[] compress(Object[] values) {
        int length = values.length;
        List<Object> nonRepeatValues = new ArrayList<>(length);
        Map<Object, SortedIntegersTokenizer> repeatValues = new HashMap<>();

        Object lastValue = null;
        SortedIntegersTokenizer lastRepeatValueIndexes = null;

        Object lastNonRepeatValue = null;
        int lastNonRepeatGlobalIndex = -1;

        // Compression algorithm
        for (int index = 0; index < length; index++) {
            Object value = values[index];
            if (!Objects.equals(value, lastValue) || lastRepeatValueIndexes == null) {
                lastRepeatValueIndexes = repeatValues.get(value);
                if (lastRepeatValueIndexes == null) {
                    if (Objects.equals(value, lastNonRepeatValue) && lastNonRepeatGlobalIndex >= 0) {
                        repeatValues.put(value, lastRepeatValueIndexes = new SortedIntegersTokenizer());
                        nonRepeatValues.remove(nonRepeatValues.size() - 1);
                        lastRepeatValueIndexes.pushInt(lastNonRepeatGlobalIndex);
                    }
                }
            }
            if (lastRepeatValueIndexes != null)
                lastRepeatValueIndexes.pushInt(index);
            else {
                nonRepeatValues.add(lastNonRepeatValue = value);
                lastNonRepeatGlobalIndex = index;
            }
            lastValue = value;
        }

        // Generating output
        Object[] compressedValues = new Object[2 + 2 * repeatValues.size() + nonRepeatValues.size()];
        int packedIndex = 0;
        compressedValues[packedIndex++] = length;
        compressedValues[packedIndex++] = repeatValues.size();
        for (Map.Entry<Object, SortedIntegersTokenizer> repeatEntry : repeatValues.entrySet()) {
            compressedValues[packedIndex++] = repeatEntry.getKey();
            compressedValues[packedIndex++] = repeatEntry.getValue().token();
        }
        for (Object nonRepeatValue : nonRepeatValues)
            compressedValues[packedIndex++] = nonRepeatValue;
        return compressedValues;
    }

    @Override
    public Object[] uncompress(Object[] compressedValues) {
        int compressedIndex = 0;
        int uncompressedLength = Numbers.intValue(compressedValues[compressedIndex++]); // May be Double instead of Integer when coming from GWT json
        int repeatValuesSize = Numbers.intValue(compressedValues[compressedIndex++]);   // May be Double instead of Integer when coming from GWT json

        Object[] uncompressedValues = new Object[uncompressedLength];
        // A repeated value can legitimately be null, so we track filled positions in a
        // separate boolean[] rather than relying on a null check.
        boolean[] filled = new boolean[uncompressedLength];

        // Scatter pass: place each repeated value directly at each of its indices. The total
        // work is the number of repeated cells (<= uncompressedLength), so the whole method is
        // O(n). The previous implementation scanned ALL repeat values for EACH output index
        // (O(n * repeatValuesSize) ≈ O(n²) when a high-cardinality column compresses into many
        // repeat entries), which blocked the event loop for seconds on large result sets.
        for (int i = 0; i < repeatValuesSize; i++) {
            Object value = compressedValues[compressedIndex++];
            SortedIntegersTokenReader indexes = new SortedIntegersTokenReader((String) compressedValues[compressedIndex++]);
            while (indexes.hasNext()) {
                int index = indexes.nextInt();
                uncompressedValues[index] = value;
                filled[index] = true;
            }
        }

        // Fill pass: the positions not covered by a repeat value are the non-repeat values, in order.
        for (int index = 0; index < uncompressedLength; index++)
            if (!filled[index])
                uncompressedValues[index] = compressedValues[compressedIndex++];

        return uncompressedValues;
    }

}
