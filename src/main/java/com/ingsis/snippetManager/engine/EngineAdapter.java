package com.ingsis.snippetManager.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.runner.CollectingEmitter;
import com.ingsis.snippetManager.engine.runner.NativeExpressionNode;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import com.ingsis.utils.runtime.DefaultRuntime;
import com.ingsis.utils.runtime.environment.Environment;
import com.ingsis.utils.type.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineAdapter {

    private final Engine engine;
    private static final Logger logger = LoggerFactory.getLogger(EngineAdapter.class);

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
            Result<String> result = engine.interpret(codeStream, version);
            if (!result.isCorrect()) {
                return new RunSnippetResponseDTO(
                        emitter.outputs(),
                        List.of(result.error())
                );
            }
            return new RunSnippetResponseDTO(emitter.outputs(), List.of());

        } catch (Exception e) {
            return new RunSnippetResponseDTO(emitter.outputs(), List.of(e.getMessage()));
        } finally {
            runtime.pop();
        }
    }

    public Result<String> analyze(InputStream src, LintSupportedRules config, Version version) {
        resetRuntime();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errStream));
        DefaultRuntime runtime = DefaultRuntime.getInstance();
        InputStream rules = lintRulesToInputStream(config);
        try {
            Result<String> r = engine.analyze(src, rules, version);
            if (!r.isCorrect()) {
                return new IncorrectResult<>(r.error() + "\n" + errStream.toString());
            }
            return new CorrectResult<>("No lint errors");
        } finally {
            runtime.pop();
            System.setErr(originalErr);
        }
    }

    public Result<String> format(InputStream src, FormatterSupportedRules formatRules, Version version) {
        resetRuntime();
        StringWriter writer = new StringWriter();
        DefaultRuntime runtime = DefaultRuntime.getInstance();
        InputStream rules = rulesToInputStream(formatRules);
        try {
            Result<String> r = engine.format(src, rules, writer, version);
            if (!r.isCorrect()) {
                return r;
            }
            return new CorrectResult<>(writer.toString());
        } finally {
            runtime.pop();
        }
    }

    private void resetRuntime() {
        DefaultRuntime runtime = DefaultRuntime.getInstance();
        while (runtime.pop().isCorrect()) {}
        runtime.setEmitter(null);
        runtime.setExecutionError(null);
        runtime.push();
    }
    private InputStream rulesToInputStream(FormatterSupportedRules rules) {
        try {
            Map<String, Object> formatterRules = new HashMap<>();
            formatterRules.put(
                    "enforce-spacing-before-colon-in-declaration",
                    rules.hasPreAscriptionSpace()
            );
            formatterRules.put(
                    "enforce-spacing-after-colon-in-declaration",
                    rules.hasPostAscriptionSpace()
            );
            formatterRules.put(
                    "enforce-spacing-around-equals",
                    rules.isAssignationSpaced()
            );
            formatterRules.put(
                    "enforce-no-spacing-around-equals",
                    !rules.isAssignationSpaced()
            );
            formatterRules.put(
                    "indent-inside-if",
                    rules.indentationInsideConditionals()
            );
            formatterRules.put(
                    "line-breaks-after-println",
                    rules.printlnSeparationLines()
            );
            formatterRules.put(
                    "mandatory-single-space-separation",
                    true
            );

            ObjectMapper mapper = new ObjectMapper();

            logger.info("ENGINE RULES JSON => {}", mapper.writeValueAsString(formatterRules));

            return new ByteArrayInputStream(
                    mapper.writeValueAsBytes(formatterRules)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert formatter rules", e);
        }
    }

    private InputStream lintRulesToInputStream(LintSupportedRules rules) {
        try {
            Map<String, Object> lintRules = new HashMap<>();

            lintRules.put(
                    "mandatory-variable-or-literal-in-println",
                    rules.mandatoryVariableOrLiteralInPrintln()
            );

            lintRules.put(
                    "mandatory-variable-or-literal-in-readInput",
                    rules.mandatoryVariableOrLiteralInReadInput()
            );

            if (rules.identifierFormat() != null) {
                lintRules.put(
                        "identifier_format",
                        rules.identifierFormat()
                );
            }

            ObjectMapper mapper = new ObjectMapper();

            logger.info("ENGINE LINT RULES JSON => {}", mapper.writeValueAsString(lintRules));

            return new ByteArrayInputStream(
                    mapper.writeValueAsBytes(lintRules)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert lint rules", e);
        }
    }
}
