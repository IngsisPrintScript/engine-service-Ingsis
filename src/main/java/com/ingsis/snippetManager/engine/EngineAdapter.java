package com.ingsis.snippetManager.engine;

import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import com.ingsis.utils.runtime.DefaultRuntime;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;

public class EngineAdapter implements EngineAdapterInterface {

    private final Engine engine;

    public EngineAdapter(Engine engine) {
        this.engine = engine;
    }

    @Override
    public RunSnippetResponseDTO execute(InputStream src, Version version) {
        OutputCollector collector = new OutputCollector();
        var originalOut = System.out;
        var originalErr = System.err;

        System.setOut(collector.out());
        System.setErr(collector.err());

        DefaultRuntime.getInstance().push();
        try {
            Result<String> result = engine.interpret(src, version);
            if (result instanceof IncorrectResult<?>(String error)) {
                collector.getErrors().add(error);
            }
        } catch (Exception e) {
            collector.getErrors().add(e.getMessage());
        } finally {
            DefaultRuntime.getInstance().pop();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return new RunSnippetResponseDTO(collector.getOutputs(), collector.getErrors());
    }

    @Override
    public Result<String> analyze(InputStream src, InputStream config, Version version) {
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errStream));

        DefaultRuntime.getInstance().push();
        try {
            Result<String> r = engine.analyze(src, config, version);
            if (!r.isCorrect()) {
                return new IncorrectResult<>(r.error() + "\n" + errStream.toString());
            }
            return new CorrectResult<>("No lint errors");
        } finally {
            DefaultRuntime.getInstance().pop();
            System.setErr(originalErr);
        }
    }

    @Override
    public Result<String> format(InputStream src, InputStream rules, Version version) {
        StringWriter writer = new StringWriter();

        DefaultRuntime.getInstance().push();
        try {
            Result<String> r = engine.format(src, rules, writer, version);
            if (!r.isCorrect()) {
                return r;
            }
            return new CorrectResult<>(writer.toString());
        } finally {
            DefaultRuntime.getInstance().pop();
        }
    }
}
