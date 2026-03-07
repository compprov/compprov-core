package io.compprov.core.variable;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;

import java.util.List;
import java.util.function.Function;

public interface WrappedVariable<T> {

    VariableTrack getVariableTrack();

    T getValue();

    ComputationContext getContext();

    /**
     * Returns function by descriptor. Function should apply operation over the arguments list
     *
     * @param operationDescriptor
     * @return
     */
    Function<List<Object>, Object> getFunction(Descriptor operationDescriptor);
}
