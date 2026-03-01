package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.Descriptor;
import io.compprov.core.Meta;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.WrappedVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class WrappedBigDecimal extends AbstractWrappedVariable<BigDecimal> {

    private static final Descriptor OP_ADD = new Descriptor("add", Meta.NO_META, Meta.NO_META);

    public WrappedBigDecimal(Context context, VariableTrack variableTrack, BigDecimal value) {
        super(context, variableTrack, value);
    }

    public WrappedBigDecimal add(WrappedBigDecimal augend,
                                 Optional<Descriptor> resultDescriptor) {
        Objects.requireNonNull(augend, "augend");
        return (WrappedBigDecimal) execute(() -> getValue().add(augend.getValue()),
                List.of(this, augend),
                OP_ADD,
                resultDescriptor);
    }

    private WrappedVariable execute(Supplier<BigDecimal> computation,
                                    List<WrappedVariable> input,
                                    Descriptor opDescriptor,
                                    Optional<Descriptor> resultDescriptor) {
        return getContext().executeOperation(
                computation,
                input,
                opDescriptor,
                resultDescriptor.orElseGet(() -> new Descriptor(
                        opDescriptor.getName() + "(" + input.toString() + ")",
                        Meta.NO_META,
                        Meta.NO_META)));
    }
}
