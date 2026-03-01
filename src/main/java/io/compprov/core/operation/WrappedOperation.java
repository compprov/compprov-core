package io.compprov.core.operation;

import io.compprov.core.variable.WrappedVariable;

import java.util.Collections;
import java.util.List;

public class WrappedOperation {

    private final OperationTrack operationTrack;
    private final List<WrappedVariable> input;
    private final WrappedVariable result;

    public WrappedOperation(OperationTrack operationTrack, List<WrappedVariable> input, WrappedVariable result) {
        this.operationTrack = operationTrack;
        this.input = Collections.unmodifiableList(input);
        this.result = result;
    }

    public OperationTrack getOperationTrack() {
        return operationTrack;
    }

    public List<WrappedVariable> getInput() {
        return input;
    }

    public WrappedVariable getResult() {
        return result;
    }
}
