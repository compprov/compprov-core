package io.compprov.core.variable;

import io.compprov.core.Context;

/**
 * Must be thread safe
 *
 * @param <T>
 */
public interface VariableWrapper<T> {

    WrappedVariable wrap(Context context, VariableTrack variableTrack, T value);
}
