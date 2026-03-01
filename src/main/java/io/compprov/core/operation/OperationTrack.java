package io.compprov.core.operation;

import io.compprov.core.Descriptor;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public final class OperationTrack {

    private final UUID id;
    private final ZonedDateTime startedAt;
    private final ZonedDateTime finishedAt;
    private final Descriptor descriptor;
    private final Class wrapperClass;

    public OperationTrack(UUID id, ZonedDateTime startedAt, ZonedDateTime finishedAt,
                          Descriptor descriptor, Class wrapperClass) {
        this.id = Objects.requireNonNull(id, "id");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.wrapperClass = Objects.requireNonNull(wrapperClass, "wrapperClass");
    }

    public UUID getId() {
        return id;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public ZonedDateTime getFinishedAt() {
        return finishedAt;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public Class getWrapperClass() {
        return wrapperClass;
    }
}
