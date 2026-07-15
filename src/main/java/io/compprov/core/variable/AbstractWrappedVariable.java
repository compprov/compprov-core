package io.compprov.core.variable;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.WrappedArgument;

import java.util.List;

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
                                      List<WrappedArgument> arguments,
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
                List.of(new WrappedArgument(argument1Name, argument1)),
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      Descriptor resultDescriptor) {
        return getContext().executeOperation(
                List.of(new WrappedArgument(argument1Name, argument1), new WrappedArgument(argument2Name, argument2)),
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      String argument3Name, WrappedVariable argument3,
                                      Descriptor resultDescriptor) {
        return getContext().executeOperation(
                List.of(new WrappedArgument(argument1Name, argument1),
                        new WrappedArgument(argument2Name, argument2),
                        new WrappedArgument(argument3Name, argument3)),
                opDescriptor,
                resultDescriptor);
    }

    protected WrappedVariable execute(Descriptor opDescriptor,
                                      String argument1Name, WrappedVariable argument1,
                                      String argument2Name, WrappedVariable argument2,
                                      String argument3Name, WrappedVariable argument3,
                                      String argument4Name, WrappedVariable argument4,
                                      Descriptor resultDescriptor) {

        return getContext().executeOperation(
                List.of(new WrappedArgument(argument1Name, argument1),
                        new WrappedArgument(argument2Name, argument2),
                        new WrappedArgument(argument3Name, argument3),
                        new WrappedArgument(argument4Name, argument4)),
                opDescriptor,
                resultDescriptor);
    }
}
