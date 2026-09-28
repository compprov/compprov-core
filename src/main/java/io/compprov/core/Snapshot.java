package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.operation.WrappedArgumentId;
import io.compprov.core.variable.VariableTrack;

import java.util.List;
import java.util.Objects;

/**
 * Point-in-time snapshot
 */
public record Snapshot(
        String version,
        Descriptor descriptor,
        List<Variable> variables,
        List<Operation> operations) {

    public static final String CURRENT_VERSION = "1.0";

    public record Variable(VariableTrack track, Object value) {
        public Variable {
            track = Objects.requireNonNull(track);
            value = Objects.requireNonNull(value);
        }
    }

    public record Operation(OperationTrack track, List<WrappedArgumentId> arguments, String resultId) {
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
        if (!CURRENT_VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }
        descriptor = Objects.requireNonNull(descriptor);
        variables = List.copyOf(Objects.requireNonNull(variables));
        operations = List.copyOf(Objects.requireNonNull(operations));
    }

    public Snapshot(Descriptor descriptor, List<Variable> variables, List<Operation> operations) {
        this(CURRENT_VERSION, descriptor, variables, operations);
    }
}
