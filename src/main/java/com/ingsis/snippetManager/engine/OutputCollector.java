package com.ingsis.snippetManager.engine;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class OutputCollector {

    private final List<String> outputs = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private final PrintStream outStream;
    private final PrintStream errStream;

    public OutputCollector() {

        this.outStream = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    outputs.add(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        });

        this.errStream = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    errors.add(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        });
    }

    public PrintStream out() {
        return outStream;
    }

    public PrintStream err() {
        return errStream;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public List<String> getErrors() {
        return errors;
    }
}