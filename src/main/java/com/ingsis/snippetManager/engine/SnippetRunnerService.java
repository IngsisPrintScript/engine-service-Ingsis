package com.ingsis.snippetManager.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SnippetRunnerService {
    private final Logger logger = Logger.getLogger(SnippetRunnerService.class.getName());
    private final AssetService assetService;
    private final LanguageEngineFactory languageEngineFactory;

    public SnippetRunnerService(AssetService assetService, LanguageEngineFactory languageEngineFactory) {
        this.assetService = assetService;
        this.languageEngineFactory = languageEngineFactory;
    }

    public RunSnippetResponseDTO execute(SupportedLanguage language, UUID snippetId, Version version) {
        Engine engine = languageEngineFactory.getEngine(language);

        ResponseEntity<String> response = assetService.getSnippet(snippetId);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return new RunSnippetResponseDTO(List.of(), List.of("Snippet not found"));
        }

        InputStream input = new ByteArrayInputStream(response.getBody().getBytes());

        OutputCollector collector = new OutputCollector();

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        System.setOut(collector.out());
        System.setErr(collector.err());

        try {
            Result<String> result = engine.interpret(input, version);

            if (!result.isCorrect()) {
                collector.getErrors().add(result.error());
            }

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        return new RunSnippetResponseDTO(collector.getOutputs(), collector.getErrors());
    }

    public Result<String> format(UUID snippetId, Version version, FormatterSupportedRules rules, SupportedLanguage language) {
        Engine engine = languageEngineFactory.getEngine(language);
        ResponseEntity<String> response = assetService.getSnippet(snippetId);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return new IncorrectResult<>("Failed to find snippet");
        }

        if (response.getBody() == null) {
            return new CorrectResult<>("Formatted");
        }

        InputStream input = new ByteArrayInputStream(response.getBody().getBytes());
        InputStream inputRules = rulesToInputStream(rules);

        StringWriter writer = new StringWriter();

        Result<String> result = engine.format(input, inputRules, writer, version);

        if (!result.isCorrect()) {
            return result;
        }
        String newContent = writer.toString();
        return new CorrectResult<>(assetService.saveSnippet(snippetId, newContent).getBody());
    }

    public Result<String> analyze(UUID snippetId, Version version, LintSupportedRules rules, SupportedLanguage language) {
        Engine engine = languageEngineFactory.getEngine(language);

        ResponseEntity<String> response = assetService.getSnippet(snippetId);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return new IncorrectResult<>(response.getBody());
        }

        if (response.getBody() == null) {
            return new CorrectResult<>("No lint errors");
        }

        InputStream input = new ByteArrayInputStream(response.getBody().getBytes());
        InputStream inputRules = rulesToInputStream(rules);
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errorStream));
        try {
            Result<String> result = engine.analyze(input, inputRules, version);

            if (!result.isCorrect()) {
                return new IncorrectResult<>(result.error() + "\n" + errorStream.toString());
            }
            return new CorrectResult<>("No lint errors");

        } finally {
            System.setErr(originalErr);
        }
    }
    public Result<RunSnippetResponseDTO> test(TestRequestDTO dto) {

        Engine engine = languageEngineFactory.getEngine(dto.language());

        ResponseEntity<String> response = assetService.getSnippet(dto.snippetId());

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return new IncorrectResult<>("Snippet not found");
        }
        String fullInput = String.join("\n", dto.inputs());
        InputStream input = new ByteArrayInputStream(fullInput.getBytes(StandardCharsets.UTF_8));
        OutputCollector collector = new OutputCollector();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(collector.out());
        System.setErr(collector.err());
        try {
            Version parsedVersion = Version.fromString(dto.version());

            Result<String> result = engine.interpret(input, parsedVersion);

            if (!result.isCorrect()) {
                return new IncorrectResult<>(result.error());
            }

        } catch (Exception e) {
            return new IncorrectResult<>("Execution error: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        List<String> realOutputs = collector.getOutputs();
        List<String> expectedOutputs = dto.outputs();

        if (!realOutputs.equals(expectedOutputs)) {
            return new IncorrectResult<>(
                    "TEST FAILED \n" +
                            "Expected outputs: " + expectedOutputs + "\n" +
                            "Actual outputs:   " + realOutputs
            );
        }
        return new CorrectResult<>(
                new RunSnippetResponseDTO(realOutputs, collector.getErrors())
        );
    }


    private InputStream rulesToInputStream(FormatterSupportedRules rules) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(rules);
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert formatter rules to InputStream", e);
        }
    }

    private InputStream rulesToInputStream(LintSupportedRules rules) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(rules);
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert formatter rules to InputStream", e);
        }
    }
}
