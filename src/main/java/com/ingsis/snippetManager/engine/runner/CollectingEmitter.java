package com.ingsis.snippetManager.engine.runner;

import com.ingsis.utils.runtime.PrintEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectingEmitter implements PrintEmitter {

    private final List<String> outputs = new ArrayList<>();

    @Override
    public void print(String value) {
        if (value == null) {
            outputs.add("null");
        } else {
            outputs.add(value);
        }
    }

    public List<String> outputs() {
        return Collections.unmodifiableList(outputs);
    }
}