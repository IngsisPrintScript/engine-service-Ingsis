package com.ingsis.snippetManager.engine;

import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.runner.CollectingEmitter;
import com.ingsis.snippetManager.engine.runner.NativeExpressionNode;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import com.ingsis.utils.runtime.DefaultRuntime;
import com.ingsis.utils.runtime.environment.Environment;
import com.ingsis.utils.type.types.Types;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineAdapter {

    private final Engine engine;

    public EngineAdapter(Engine engine) {
        this.engine = engine;
    }

    public RunSnippetResponseDTO execute(String code, Version version, List<String> inputs, Map<String, String> envs) {

        CollectingEmitter emitter = new CollectingEmitter();
        DefaultRuntime runtime = DefaultRuntime.getInstance();
        runtime.setEmitter(emitter);

        try {
            runtime.push();
            Environment env = runtime.getCurrentEnvironment();

            for (var e : envs.entrySet()) {
                env.createVariable(e.getKey(), Types.STRING, e.getValue(), false);
            }

            AtomicInteger index = new AtomicInteger(0);

            /* readInput() */
            env.createFunction("readInput", new LinkedHashMap<>(), Types.STRING);
            env.updateFunction("readInput", List.of(
                    new NativeExpressionNode(() ->
                            index.get() < inputs.size()
                                    ? inputs.get(index.getAndIncrement())
                                    : ""
                    )
            ));
            env.createFunction("readNumber", new LinkedHashMap<>(), Types.NUMBER);
            env.updateFunction("readNumber", List.of(
                    new NativeExpressionNode(() -> {
                        if (index.get() >= inputs.size()) {
                            return 0.0;
                        }
                        try {
                            return Double.parseDouble(inputs.get(index.getAndIncrement()));
                        } catch (NumberFormatException e) {
                            return 0.0;
                        }
                    })
            ));

            /* readEnv(key) */
            LinkedHashMap<String, Types> args = new LinkedHashMap<>();
            args.put("key", Types.STRING);

            env.createFunction("readEnv", args, Types.STRING);
            env.updateFunction("readEnv", List.of(
                    new NativeExpressionNode(() -> {
                        var keyResult = env.readVariable("key");

                        if (!keyResult.isCorrect() || keyResult.result().value() == null) {
                            return "";
                        }

                        String key = keyResult.result().value().toString();
                        var valueResult = env.readVariable(key);

                        if (!valueResult.isCorrect() || valueResult.result().value() == null) {
                            return "";
                        }

                        return valueResult.result().value().toString();
                    })
            ));

            InputStream codeStream = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));

            engine.interpret(codeStream, version);

            return new RunSnippetResponseDTO(emitter.outputs(), List.of());

        } catch (Exception e) {
            return new RunSnippetResponseDTO(emitter.outputs(), List.of(e.getMessage()));
        } finally {
            runtime.pop();
        }
    }

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
