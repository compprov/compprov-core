package io.compprov.core.operation;

import java.util.LinkedHashMap;
import java.util.Objects;

public class WrappedOperation {

    private final OperationTrack operationTrack;
    private final LinkedHashMap<String, String> arguments;//name=variableId
    private final String resultId;

    public WrappedOperation(OperationTrack operationTrack,
                            LinkedHashMap<String, String> arguments, String resultId) {
        this.operationTrack = Objects.requireNonNull(operationTrack);
        this.arguments = new LinkedHashMap<>();
        Objects.requireNonNull(arguments).forEach((k, v) -> this.arguments.put(k, v));
        this.resultId = Objects.requireNonNull(resultId);
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Operation must contain at least one argument");
        }
    }

    public static WrappedOperation operation(
            OperationTrack operationTrack,
            LinkedHashMap<String, String> arguments,
            String resultId) {
        return new WrappedOperation(operationTrack, arguments, resultId);
    }

    public OperationTrack getOperationTrack() {
        return operationTrack;
    }

    /**
     * argumentName->variableId
     *
     * @return
     */
    public LinkedHashMap<String, String> getArguments() {
        final var result = new LinkedHashMap<String, String>();
        arguments.forEach((k, v) -> result.put(k, v));
        return result;
    }

    public String getResultId() {
        return resultId;
    }
}
