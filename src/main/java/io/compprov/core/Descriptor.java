package io.compprov.core;

import java.util.Objects;

public final class Descriptor {

    private final String name;
    private final Meta description;
    private final Meta origin;

    public Descriptor(String name, Meta description, Meta origin) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    public static Descriptor descriptor(String name) {
        return new Descriptor(name, Meta.NO_META, Meta.NO_META);
    }

    public static Descriptor descriptor(String shortName,
                                        Meta description,
                                        Meta origin) {
        return new Descriptor(shortName, description, origin);
    }

    public String getName() {
        return name;
    }

    public Meta getDescription() {
        return description;
    }

    public Meta getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return "Descriptor{" +
                "name='" + name + '\'' +
                ", description=" + description +
                ", origin=" + origin +
                '}';
    }
}
