package io.compprov.core.meta;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Immutable marker that carries a semantic description of a tracked variable/operation.
 *
 * <p>Feel free to add static creators for this class that include all mandatory metadata for your environment</p>
 */
public final class Meta {

    public static final Meta NO_META = new Meta(new LinkedHashMap<>());

    private final LinkedHashMap<String, Object> parameters;
    private int hash;

    public Meta(LinkedHashMap<String, Object> parameters) {
        Objects.requireNonNull(parameters, "parameters");
        this.parameters = new LinkedHashMap<>();
        this.parameters.putAll(parameters);
    }

    public static Meta formula(String formula) {
        return of("formula", formula);
    }

    public static Meta meta(String description) {
        return of("description", description);
    }

    public static Meta meta(String description, String origin) {
        return of("description", description,
                "origin", origin);
    }

    public static Meta of(String k, Object v) {
        return new Meta(linkedMap(k, v));
    }

    public static Meta of(String k1, Object v1, String k2, Object v2) {
        return new Meta(linkedMap(k1, v1, k2, v2));
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return new Meta(linkedMap(k1, v1, k2, v2, k3, v3));
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        return new Meta(linkedMap(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4,
                          String k5, Object v5) {
        return new Meta(linkedMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4,
                          String k5, Object v5, String k6, Object v6) {
        return new Meta(linkedMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6));
    }

    private static LinkedHashMap<String, Object> linkedMap(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs must contain an even number of elements (key/value)");
        }
        var m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], pairs[i + 1]);
        }
        return m;
    }

    public String first() {
        if (parameters.isEmpty()) {
            return "";
        }
        final var value = parameters.values().iterator().next();
        return (value == null) ? "" : value.toString();
    }

    public LinkedHashMap<String, Object> getParameters() {
        final var result = new LinkedHashMap<String, Object>();
        result.putAll(parameters);
        return result;
    }

    @Override
    public String toString() {
        return "Meta" + parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meta meta = (Meta) o;
        return Objects.equals(parameters, meta.parameters);
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = Objects.hash(parameters);
        }
        return hash;
    }
}
