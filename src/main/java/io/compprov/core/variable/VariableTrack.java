package io.compprov.core.variable;

import io.compprov.core.Descriptor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class VariableTrack {

    private final UUID id;
    private final Instant createdAt;
    private final VariableKind kind;
    private final Descriptor descriptor;

    public VariableTrack(UUID id, Instant createdAt,
                         VariableKind kind, Descriptor descriptor) {
        this.id = Objects.requireNonNull(id, "id");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public VariableKind getKind() {
        return kind;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }
}
