package io.compprov.core.variable;

import io.compprov.core.meta.Descriptor;

public record ValueWithDescriptor(Descriptor descriptor, Object value) {
}
