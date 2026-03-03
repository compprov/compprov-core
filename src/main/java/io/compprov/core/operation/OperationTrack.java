package io.compprov.core.operation;

import io.compprov.core.Descriptor;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class OperationTrack {

    private final String id;
    private final int numericId;
    private final ZonedDateTime startedAt;
    private final ZonedDateTime finishedAt;
    private final Descriptor descriptor;
    private final Class wrapperClass;

    public OperationTrack(int numericId, ZonedDateTime startedAt, ZonedDateTime finishedAt,
                          Descriptor descriptor, Class wrapperClass) {
        this.id = "op_%s".formatted(numericId);
        this.numericId = numericId;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.wrapperClass = Objects.requireNonNull(wrapperClass, "wrapperClass");
    }

    public String getId() {
        return id;
    }

    public int getNumericId() {
        return numericId;
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
