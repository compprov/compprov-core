package io.compprov.core;

/**
 * Marker interface that carries a semantic description of a tracked variable/operation.
 *
 * <p>Implement this interface to attach human or machine-readable context
 * to a {@link Descriptor}. Examples: a business-domain explanation,
 * a unit annotation, or a validation rule.</p>
 *
 * <p>When no description is needed, use NO_META</p>
 */
public interface Meta {

    public static Meta NO_META = new Meta() {
    };
}
