package io.compprov.core.operation;

import java.util.Collections;
import java.util.List;

public class WrappedOperation {

    private final OperationTrack operationTrack;
    private final List<String> inputIds;
    private final String resultId;

    public WrappedOperation(OperationTrack operationTrack, List<String> inputIds, String resultId) {
        this.operationTrack = operationTrack;
        this.inputIds = Collections.unmodifiableList(inputIds);
        this.resultId = resultId;
    }

    public OperationTrack getOperationTrack() {
        return operationTrack;
    }

    public List<String> getInputIds() {
        return inputIds;
    }

    public String getResultId() {
        return resultId;
    }
}
