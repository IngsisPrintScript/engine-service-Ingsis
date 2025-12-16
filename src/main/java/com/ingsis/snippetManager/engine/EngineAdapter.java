package com.ingsis.snippetManager.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.runner.CollectingEmitter;
import com.ingsis.snippetManager.engine.runner.InputSupplierAdapter;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineAdapter {

    private final Engine engine;
    private static final Logger logger = LoggerFactory.getLogger(EngineAdapter.class);

    public EngineAdapter(Engine engine) {
        this.engine = engine;
    }

    public RunSnippetResponseDTO execute(String code, Version version, List<String> inputs, Map<String, String> envs) {
        CollectingEmitter emitter = new CollectingEmitter();
        InputSupplierAdapter supplierAdapter = new InputSupplierAdapter(inputs);
        try {
            engine.interpret(version, emitter, supplierAdapter, new ByteArrayInputStream(code.getBytes()));
            return new RunSnippetResponseDTO(emitter.outputs(), List.of());
        } catch (Exception e) {
            return new RunSnippetResponseDTO(emitter.outputs(), List.of(e.getMessage()));
        }
    }

    public Result<String> analyze(InputStream src, LintSupportedRules config, Version version) {
        Result<String> r = engine.analyze(src, lintRulesToInputStream(config), version);
        if (!r.isCorrect()) {
            return new IncorrectResult<>(r.error());
        }
        return new CorrectResult<>("No lint errors");

    }

    public Result<String> format(InputStream src, FormatterSupportedRules formatRules, Version version) {
        StringWriter writer = new StringWriter();
        InputStream rules = rulesToInputStream(formatRules);
        Result<String> r = engine.format(src, rules, writer, version);
        if (!r.isCorrect()) {
            return r;
        }
        return new CorrectResult<>(writer.toString());
    }

    private InputStream rulesToInputStream(FormatterSupportedRules rules) {
        try {
            Map<String, Object> formatterRules = new HashMap<>();
            formatterRules.put("enforce-spacing-before-colon-in-declaration", rules.hasPreAscriptionSpace());
            formatterRules.put("enforce-spacing-after-colon-in-declaration", rules.hasPostAscriptionSpace());
            formatterRules.put("enforce-spacing-around-equals", rules.isAssignationSpaced());
            formatterRules.put("enforce-no-spacing-around-equals", !rules.isAssignationSpaced());
            formatterRules.put("indent-inside-if", rules.indentationInsideConditionals());
            formatterRules.put("line-breaks-after-println", rules.printlnSeparationLines());
            formatterRules.put("mandatory-single-space-separation", true);

            ObjectMapper mapper = new ObjectMapper();

            logger.info("ENGINE RULES JSON => {}", mapper.writeValueAsString(formatterRules));

            return new ByteArrayInputStream(mapper.writeValueAsBytes(formatterRules));

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert formatter rules", e);
        }
    }

    private InputStream lintRulesToInputStream(LintSupportedRules rules) {
        try {
            Map<String, Object> lintRules = new HashMap<>();

            lintRules.put("mandatory-variable-or-literal-in-println", rules.mandatoryVariableOrLiteralInPrintln());

            lintRules.put("mandatory-variable-or-literal-in-readInput", rules.mandatoryVariableOrLiteralInReadInput());

            if (rules.identifierFormat() != null) {
                lintRules.put("identifier_format", rules.identifierFormat());
            }

            ObjectMapper mapper = new ObjectMapper();

            logger.info("ENGINE LINT RULES JSON => {}", mapper.writeValueAsString(lintRules));

            return new ByteArrayInputStream(mapper.writeValueAsBytes(lintRules));

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert lint rules", e);
        }
    }
}
