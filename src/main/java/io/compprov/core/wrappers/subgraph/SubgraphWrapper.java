package io.compprov.core.wrappers.subgraph;

import io.compprov.core.ComputationContext;
import io.compprov.core.Subgraph;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

public class SubgraphWrapper implements VariableWrapper<Subgraph> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, Subgraph subgraph) {
        return new WrappedSubgraph(context, variableTrack, subgraph);
    }
}
