package io.compprov.core.variable;

import io.compprov.core.Descriptor;
import io.compprov.core.meta.Meta;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class VariableTrack {

    private final String id;
    private final int numericId;
    private final ZonedDateTime createdAt;
    private final VariableKind kind;
    private final Descriptor descriptor;
    private final Class valueClass;

    public VariableTrack(int numericId, ZonedDateTime createdAt, VariableKind kind,
                         Descriptor descriptor, Class valueClass) {
        this.id = switch (kind) {
            case INPUT -> "i_%s".formatted(numericId);
            case OUTPUT -> "o_%s".formatted(numericId);
        };
        this.numericId = numericId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.descriptor = descriptor == null ? new Descriptor(id, Meta.NO_META, Meta.NO_META) : descriptor;
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

    public Class getValueClass() {
        return valueClass;
    }
}
