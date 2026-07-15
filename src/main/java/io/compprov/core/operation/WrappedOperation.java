package io.compprov.core.operation;

import io.compprov.core.Snapshot;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WrappedOperation {

    private final OperationTrack operationTrack;
    private final List<? extends OperationArgument> arguments;//{name,variableId}
    private final String resultId;

    public WrappedOperation(OperationTrack operationTrack,
                            List<? extends OperationArgument> arguments, String resultId) {
        this.operationTrack = Objects.requireNonNull(operationTrack);
        this.arguments = Collections.unmodifiableList(arguments);
        this.resultId = Objects.requireNonNull(resultId);
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Operation must contain at least one argument");
        }
    }

    public static WrappedOperation operation(
            OperationTrack operationTrack,
            List<? extends OperationArgument> arguments,
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
    public List<? extends OperationArgument> getArguments() {
        return arguments;
    }

    public String getResultId() {
        return resultId;
    }

    public Snapshot.Operation snapshot() {
        return new Snapshot.Operation(operationTrack,
                arguments.stream().map(arg -> new WrappedArgumentId(arg.metaName(), arg.variableId())).toList(),
                resultId);
    }
}
