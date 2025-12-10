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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SnippetRunnerService {

    private final AssetService assetService;
    private final LanguageEngineFactory languageEngineFactory;

    public SnippetRunnerService(AssetService assetService, LanguageEngineFactory languageEngineFactory) {
        this.assetService = assetService;
        this.languageEngineFactory = languageEngineFactory;
    }

    public RunSnippetResponseDTO execute(SupportedLanguage language, UUID snippetId, Version version) {
        InputStream src = loadSnippet(snippetId);
        if (src == null) {
            return new RunSnippetResponseDTO(List.of(), List.of("Snippet not found"));
        }

        EngineAdapter adapter = createAdapter(language);
        return adapter.execute(src, version);
    }

    public Result<String> format(UUID snippetId, Version version, FormatterSupportedRules rules,
            SupportedLanguage language) {
        InputStream src = loadSnippet(snippetId);
        if (src == null) {
            return new IncorrectResult<>("Snippet not found");
        }

        EngineAdapter adapter = createAdapter(language);
        InputStream rulesStream = rulesToInputStream(rules);

        Result<String> formattedResult = adapter.format(src, rulesStream, version);
        if (!formattedResult.isCorrect()) {
            return formattedResult;
        }

        return saveSnippet(snippetId, formattedResult.result());
    }

    public Result<String> analyze(UUID snippetId, Version version, LintSupportedRules rules,
            SupportedLanguage language) {
        InputStream src = loadSnippet(snippetId);
        if (src == null) {
            return new IncorrectResult<>("Snippet not found");
        }

        EngineAdapter adapter = createAdapter(language);
        InputStream rulesStream = rulesToInputStream(rules);

        return adapter.analyze(src, rulesStream, version);
    }

    public Result<List<String>> validate(UUID snippetId, SupportedLanguage language, Version version) {
        RunSnippetResponseDTO exec = execute(language, snippetId, version);

        if (exec.errors().isEmpty()) {
            return new CorrectResult<>(List.of());
        }
        return new IncorrectResult<>("Invalid snippet:\n" + String.join("\n", exec.errors()));
    }

    public Result<RunSnippetResponseDTO> test(TestRequestDTO dto) {
        String joinedInput = String.join("\n", dto.inputs());
        InputStream inputStream = new ByteArrayInputStream(joinedInput.getBytes(StandardCharsets.UTF_8));

        EngineAdapter adapter = createAdapter(dto.language());
        Version parsedVersion = Version.fromString(dto.version());

        RunSnippetResponseDTO exec = adapter.execute(inputStream, parsedVersion);
        if (!exec.errors().isEmpty()) {
            return new IncorrectResult<>("Execution error:\n" + String.join("\n", exec.errors()));
        }

        if (!exec.outputs().equals(dto.outputs())) {
            return new IncorrectResult<>(
                    "TEST FAILED\n" + "Expected: " + dto.outputs() + "\n" + "Actual:   " + exec.outputs());
        }

        return new CorrectResult<>(exec);
    }

    private EngineAdapter createAdapter(SupportedLanguage language) {
        Engine engine = languageEngineFactory.getEngine(language);
        return new EngineAdapter(engine);
    }

    private InputStream loadSnippet(UUID id) {
        ResponseEntity<String> response = assetService.getSnippet(id);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }
        return new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8));
    }

    private Result<String> saveSnippet(UUID id, String newContent) {
        try {
            return new CorrectResult<>(assetService.saveSnippet(id, newContent).getBody());
        } catch (Exception e) {
            return new IncorrectResult<>("Failed to save snippet");
        }
    }

    private InputStream rulesToInputStream(Object rulesObj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(rulesObj);
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert rules JSON", e);
        }
    }
}
