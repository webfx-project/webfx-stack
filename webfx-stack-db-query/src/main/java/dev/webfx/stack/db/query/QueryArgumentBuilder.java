package dev.webfx.stack.db.query;

import dev.webfx.stack.db.datascope.DataScope;

/**
 * @author Bruno Salmon
 */
public final class QueryArgumentBuilder {

    private QueryArgument originalArgument;
    private Object dataSourceId;
    private DataScope dataScope;
    private String language;
    private String statement;
    private Object[] parameters;
    private String[] parameterNames;
    private boolean sendMetadata;
    private boolean hasDqlRuntime = true;
    private int priority = QueryArgument.STANDARD_PRIORITY;
    private int callId;
    private int callSeq;
    private boolean shedWhenBusy;

    public QueryArgumentBuilder setOriginalArgument(QueryArgument originalArgument) {
        this.originalArgument = originalArgument;
        return this;
    }

    public QueryArgumentBuilder setDataSourceId(Object dataSourceId) {
        this.dataSourceId = dataSourceId;
        return this;
    }

    public QueryArgumentBuilder setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
        return this;
    }

    public QueryArgumentBuilder addDataScope(DataScope dataScope) {
        return setDataScope(DataScope.concat(this.dataScope, dataScope));
    }

    public QueryArgumentBuilder setLanguage(String language) {
        this.language = language;
        return this;
    }

    public QueryArgumentBuilder setStatement(String statement) {
        this.statement = statement;
        return this;
    }

    public QueryArgumentBuilder setParameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public QueryArgumentBuilder setParameterNames(String... parameterNames) {
        this.parameterNames = parameterNames;
        return this;
    }

    public QueryArgumentBuilder setSendMetadata(boolean sendMetadata) {
        this.sendMetadata = sendMetadata;
        return this;
    }

    public QueryArgumentBuilder setHasDqlRuntime(boolean hasDqlRuntime) {
        this.hasDqlRuntime = hasDqlRuntime;
        return this;
    }

    public QueryArgumentBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public QueryArgumentBuilder setCallId(int callId) {
        this.callId = callId;
        return this;
    }

    public QueryArgumentBuilder setCallSeq(int callSeq) {
        this.callSeq = callSeq;
        return this;
    }

    public QueryArgumentBuilder setShedWhenBusy(boolean shedWhenBusy) {
        this.shedWhenBusy = shedWhenBusy;
        return this;
    }

    public QueryArgumentBuilder copy(QueryArgument argument) {
        return setOriginalArgument(argument)
            .setDataSourceId(argument.getDataSourceId())
            .setDataScope(argument.getDataScope())
            .setLanguage(argument.getLanguage())
            .setStatement(argument.getStatement())
            .setParameters(argument.getParameters())
            .setParameterNames(argument.getParameterNames())
            .setSendMetadata(argument.isSendMetadata())
            .setHasDqlRuntime(argument.isHasDqlRuntime())
            .setPriority(argument.getPriority())
            .setCallId(argument.getCallId())
            .setCallSeq(argument.getCallSeq())
            .setShedWhenBusy(argument.isShedWhenBusy())
            ;
    }

    public QueryArgument build() {
        return new QueryArgument(originalArgument, dataSourceId, dataScope, language, statement, parameters, parameterNames, sendMetadata, hasDqlRuntime, priority, callId, callSeq, shedWhenBusy);
    }
}
