package io.compprov.core.variable;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class VariableTrack {

    private final String id;
    private final int numericId;
    private final ZonedDateTime createdAt;
    private final VariableKind kind;
    private final Descriptor descriptor;
    private final String valueClass;

    public VariableTrack(int numericId, ZonedDateTime createdAt, VariableKind kind,
                         Descriptor descriptor, String valueClass) {
        this.id = switch (kind) {
            case INPUT -> "i_%s".formatted(numericId);
            case OUTPUT -> "o_%s".formatted(numericId);
        };
        this.numericId = numericId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.descriptor = descriptor == null ? new Descriptor(id, Meta.NO_META) : descriptor;
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
    }

    public String getId() {
        return id;
    }

    public int getNumericId() {
        return numericId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public VariableKind getKind() {
        return kind;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public String getValueClass() {
        return valueClass;
    }

    @Override
    public String toString() {
        return "VariableTrack{" +
                "id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableTrack that = (VariableTrack) o;
        return numericId == that.numericId && Objects.equals(id, that.id) && Objects.equals(createdAt.toInstant(), that.createdAt.toInstant()) && kind == that.kind && Objects.equals(descriptor, that.descriptor) && Objects.equals(valueClass, that.valueClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numericId, createdAt, kind, descriptor, valueClass);
    }
}
