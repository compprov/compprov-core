package io.compprov.core.meta;

import java.util.Objects;

public final class Descriptor {

    private final String name;
    private final Meta meta;

    public Descriptor(String name, Meta meta) {
        this.name = Objects.requireNonNull(name, "name");
        this.meta = Objects.requireNonNull(meta, "meta");
    }

    public static Descriptor descriptor(String name) {
        return new Descriptor(name, Meta.NO_META);
    }

    public static Descriptor descriptor(String name, Meta meta) {
        return new Descriptor(name, meta);
    }

    public String getName() {
        return name;
    }

    public Meta getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return "Descriptor{" +
                "name='" + name + '\'' +
                ", meta=" + meta +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Descriptor that = (Descriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(meta, that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, meta);
    }
}
