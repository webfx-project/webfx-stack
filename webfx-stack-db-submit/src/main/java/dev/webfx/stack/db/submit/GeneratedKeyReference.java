package dev.webfx.stack.db.submit;

/**
 * @author Bruno Salmon
 */
public final class GeneratedKeyReference {

    private final int statementBatchIndex;
    private final int generatedKeyIndex;

    public GeneratedKeyReference(int statementBatchIndex) {
        this(statementBatchIndex, 0);
    }

    public GeneratedKeyReference(int statementBatchIndex, int generatedKeyIndex) {
        this.statementBatchIndex = statementBatchIndex;
        this.generatedKeyIndex = generatedKeyIndex;
    }

    public int getStatementBatchIndex() {
        return statementBatchIndex;
    }

    public int getGeneratedKeyIndex() {
        return generatedKeyIndex;
    }

    @Override
    public String toString() {
        return "GeneratedKeyReference{" +
               "statementBatchIndex=" + statementBatchIndex +
               ", generatedKeyIndex=" + generatedKeyIndex +
               '}';
    }
}
