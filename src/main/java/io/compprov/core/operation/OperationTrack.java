package io.compprov.core.operation;

import io.compprov.core.meta.Descriptor;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class OperationTrack {

    private final String id;
    private final int numericId;
    private final ZonedDateTime startedAt;
    private final ZonedDateTime finishedAt;
    private final Descriptor descriptor;
    private final String wrapperClass;

    public OperationTrack(int numericId, ZonedDateTime startedAt, ZonedDateTime finishedAt,
                          Descriptor descriptor, String wrapperClass) {
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

    public String getWrapperClass() {
        return wrapperClass;
    }

    @Override
    public String toString() {
        return "OperationTrack{" +
                "id='" + id + '\'' +
                ", descriptor=" + descriptor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationTrack that = (OperationTrack) o;
        return numericId == that.numericId
                && Objects.equals(id, that.id)
                && Objects.equals(startedAt.toInstant(), that.startedAt.toInstant())
                && Objects.equals(finishedAt.toInstant(), that.finishedAt.toInstant())
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(wrapperClass, that.wrapperClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numericId, startedAt, finishedAt, descriptor, wrapperClass);
    }
}
