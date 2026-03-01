package io.compprov.core.variable;

import io.compprov.core.Context;
import io.compprov.core.Descriptor;

public interface WrappedVariable<T> {

    VariableTrack getVariableTrack();

    default Descriptor getDescriptor() {
        return getVariableTrack().getDescriptor();
    }

    public T getValue();

    public Context getContext();
}
