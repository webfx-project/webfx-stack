package dev.webfx.stack.db.submit;

/**
 * @author Bruno Salmon
 */
public final class SubmitResult {

    private final int rowCount;
    private final Object[] generatedKeys;

    public SubmitResult(int rowCount, Object[] generatedKeys) {
        this.rowCount = rowCount;
        this.generatedKeys = generatedKeys;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Object[] getGeneratedKeys() {
        return generatedKeys;
    }

}
