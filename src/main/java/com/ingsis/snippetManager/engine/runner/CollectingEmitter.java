package com.ingsis.snippetManager.engine.runner;

import com.ingsis.utils.evalstate.io.OutputEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectingEmitter implements OutputEmitter {

    private final List<String> outputs = new ArrayList<>();

    public List<String> outputs() {
        return Collections.unmodifiableList(outputs);
    }

    @Override
    public void emit(String arg0) {
        if (arg0 == null) {
            outputs.add("null");
        } else {
            outputs.add(arg0);
        }
    }
}
