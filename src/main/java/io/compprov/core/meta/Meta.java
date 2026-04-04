package io.compprov.core.meta;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Immutable marker that carries a semantic description of a tracked variable/operation.
 *
 * <p>Feel free to add static creators for this class that include all mandatory metadata for your environment</p>
 */
public class Meta {

    public static final Meta NO_META = new Meta(new LinkedHashMap<>());

    protected final LinkedHashMap<String, Object> parameters;

    public Meta(LinkedHashMap<String, Object> parameters) {
        Objects.requireNonNull(parameters, "parameters");
        this.parameters = new LinkedHashMap<>();
        parameters.forEach((k, v) -> this.parameters.put(k, v));
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
        return new Meta(new LinkedHashMap() {{
            put(k, v);
        }});
    }

    public static Meta of(String k1, Object v1, String k2, Object v2) {
        return new Meta(new LinkedHashMap() {{
            put(k1, v1);
            put(k2, v2);
        }});
    }


    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return new Meta(new LinkedHashMap() {{
            put(k1, v1);
            put(k2, v2);
            put(k3, v3);
        }});
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        return new Meta(new LinkedHashMap() {{
            put(k1, v1);
            put(k2, v2);
            put(k3, v3);
            put(k4, v4);
        }});
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4,
                          String k5, Object v5) {
        return new Meta(new LinkedHashMap() {{
            put(k1, v1);
            put(k2, v2);
            put(k3, v3);
            put(k4, v4);
            put(k5, v5);
        }});
    }

    public static Meta of(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4,
                          String k5, Object v5, String k6, Object v6) {
        return new Meta(new LinkedHashMap() {{
            put(k1, v1);
            put(k2, v2);
            put(k3, v3);
            put(k4, v4);
            put(k5, v5);
            put(k6, v6);
        }});
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
        parameters.forEach(result::put);
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
        return Objects.hash(parameters);
    }
}
