package dev.webfx.stack.db.submit;

import dev.webfx.stack.db.datascope.DataScope;

/**
 * @author Bruno Salmon
 */
public final class SubmitArgumentBuilder {

    private SubmitArgument originalArgument;
    private Object dataSourceId;
    private DataScope dataScope;
    private boolean returnGeneratedKeys;
    private String language;
    private String statement;
    private Object[] parameters;
    private int priority = SubmitArgument.STANDARD_PRIORITY;
    private boolean transactionPreamble;

    public SubmitArgumentBuilder setOriginalArgument(SubmitArgument originalArgument) {
        this.originalArgument = originalArgument;
        return this;
    }

    public SubmitArgumentBuilder setDataSourceId(Object dataSourceId) {
        this.dataSourceId = dataSourceId;
        return this;
    }

    public SubmitArgumentBuilder setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
        return this;
    }

    public SubmitArgumentBuilder addDataScope(DataScope dataScope) {
        return setDataScope(DataScope.concat(this.dataScope, dataScope));
    }

    public SubmitArgumentBuilder setReturnGeneratedKeys(boolean returnGeneratedKeys) {
        this.returnGeneratedKeys = returnGeneratedKeys;
        return this;
    }

    public SubmitArgumentBuilder setLanguage(String language) {
        this.language = language;
        return this;
    }

    public SubmitArgumentBuilder setStatement(String statement) {
        this.statement = statement;
        return this;
    }

    public SubmitArgumentBuilder setParameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    /** Marks this entry as a request for the application's transaction preamble — see
     * {@link SubmitArgument#isTransactionPreamble()}. Statement and parameters are then ignored. */
    public SubmitArgumentBuilder setTransactionPreamble(boolean transactionPreamble) {
        this.transactionPreamble = transactionPreamble;
        return this;
    }

    public SubmitArgumentBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public SubmitArgumentBuilder copy(SubmitArgument argument) {
        return setOriginalArgument(argument)
                .setDataSourceId(argument.getDataSourceId())
                .setDataScope(argument.getDataScope())
                .setReturnGeneratedKeys(argument.returnGeneratedKeys())
                .setLanguage(argument.getLanguage())
                .setStatement(argument.getStatement())
                .setParameters(argument.getParameters())
                .setPriority(argument.getPriority())
                .setTransactionPreamble(argument.isTransactionPreamble());
    }

    public SubmitArgument build() {
        return new SubmitArgument(originalArgument, dataSourceId, dataScope, returnGeneratedKeys, language, statement, parameters, priority, transactionPreamble);
    }
}
