package io.compprov.core.operation;

import io.compprov.core.Descriptor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OperationTrack {

    private final UUID id;
    private final Instant startedAt;
    private final Instant finishedAt;

    private final Descriptor descriptor;

    public OperationTrack(UUID id, Instant startedAt, Instant finishedAt,
                          Descriptor descriptor) {
        this.id = Objects.requireNonNull(id, "id");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.descriptor = Objects.requireNonNull(descriptor, "operationDescriptor");
    }

    public UUID getId() {
        return id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }
}
