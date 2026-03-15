package io.compprov.core.variable;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractWrappedVariable<T> implements WrappedVariable<T> {
    private final ComputationContext context;
    private final VariableTrack variableTrack;
    private final T value;


    public AbstractWrappedVariable(ComputationContext context, VariableTrack variableTrack, T value) {
        this.context = context;
        this.variableTrack = variableTrack;
        this.value = value;
    }

    @Override
    public VariableTrack getVariableTrack() {
        return variableTrack;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public ComputationContext getContext() {
        return context;
    }

    public String getWrappedClass() {
        return value.getClass().getSimpleName();
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      LinkedHashMap<String, WrappedVariable> arguments,
                                      Descriptor resultDescriptor) {
        return getContext().executeOperation(
                arguments,
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      Descriptor resultDescriptor) {
        return getContext().executeOperation(
                new LinkedHashMap<String, WrappedVariable>() {{
                    put(argument1Name, argument1);
                }},
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      Descriptor resultDescriptor) {
        final var arguments = new LinkedHashMap<String, WrappedVariable>() {{
            put(argument1Name, argument1);
            put(argument2Name, argument2);
        }};
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("Argument names must be unique");
        }
        return getContext().executeOperation(
                arguments,
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      String argument3Name, WrappedVariable argument3,
                                      Descriptor resultDescriptor) {
        final var arguments = new LinkedHashMap<String, WrappedVariable>() {{
            put(argument1Name, argument1);
            put(argument2Name, argument2);
            put(argument3Name, argument3);
        }};
        if (arguments.size() != 3) {
            throw new IllegalArgumentException("Argument names must be unique");
        }
        return getContext().executeOperation(
                arguments,
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      String argument3Name, WrappedVariable argument3,
                                      String argument4Name, WrappedVariable argument4,
                                      Descriptor resultDescriptor) {

        final var arguments = new LinkedHashMap<String, WrappedVariable>() {{
            put(argument1Name, argument1);
            put(argument2Name, argument2);
            put(argument3Name, argument3);
            put(argument4Name, argument4);
        }};
        if (arguments.size() != 4) {
            throw new IllegalArgumentException("Argument names must be unique");
        }
        return getContext().executeOperation(
                arguments,
                opDescriptor,
                resultDescriptor);
    }
}
