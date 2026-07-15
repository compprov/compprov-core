package io.compprov.core.operation;

public record WrappedArgumentId(String key, String value) implements OperationArgument {
    @Override
    public String metaName() {
        return key;
    }

    @Override
    public String variableId() {
        return value;
    }
}
