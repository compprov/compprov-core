package io.compprov.core.variable;

import io.compprov.core.ComputationContext;

/**
 * Must be thread safe
 *
 * @param <T>
 */
public interface VariableWrapper<T> {
    WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, T value);
}
