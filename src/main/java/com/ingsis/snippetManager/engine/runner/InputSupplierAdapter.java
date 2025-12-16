package com.ingsis.snippetManager.engine.runner;

import com.ingsis.utils.evalstate.io.InputSupplier;
import com.ingsis.utils.value.Value;
import java.util.List;

public class InputSupplierAdapter implements InputSupplier {
    private final List<String> inputs;
    private int index;

    public InputSupplierAdapter(List<String> inputs) {
        this.inputs = inputs;
        this.index = 0;
    }

    @Override
    public Value supply() {
        return new Value.StringValue(inputs.get(index++));
    }

}
