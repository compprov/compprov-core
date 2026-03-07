package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.variable.VariableTrack;

import java.util.*;

/**
 * Point-in-time snapshot
 */
public record Snapshot(Descriptor descriptor,
                       List<Variable> variables,
                       List<Operation> operations) {

    public record Variable(VariableTrack track, Object value) {
        public Variable {
            track = Objects.requireNonNull(track);
            value = Objects.requireNonNull(value);
        }
    }

    public record Operation(OperationTrack track, LinkedHashMap<String, String> arguments, String resultId) {
        public Operation {
            track = Objects.requireNonNull(track);
            arguments = Objects.requireNonNull(arguments);
            resultId = Objects.requireNonNull(resultId);
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException("Operation must contain at least one argument");
            }
        }
    }

    public Snapshot {
        variables = List.copyOf(Objects.requireNonNull(variables));
        operations = List.copyOf(Objects.requireNonNull(operations));
    }
}
