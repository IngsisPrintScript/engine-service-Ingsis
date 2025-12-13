package com.ingsis.snippetManager.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.engine.Engine;
import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.dto.response.TestResponseDTO;
import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.IncorrectResult;
import com.ingsis.utils.result.Result;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SnippetRunnerService {

    private final AssetService assetService;
    private final LanguageEngineFactory languageEngineFactory;
    private static final Logger logger = LoggerFactory.getLogger(SnippetRunnerService.class);

    public SnippetRunnerService(AssetService assetService, LanguageEngineFactory languageEngineFactory) {
        this.assetService = assetService;
        this.languageEngineFactory = languageEngineFactory;
    }

    public RunSnippetResponseDTO execute(SupportedLanguage language, UUID snippetId, Version version,
            List<String> inputs, Map<String, String> envs) {
        String code = assetService.getSnippet(snippetId).getBody();
        if (code == null) {
            return new RunSnippetResponseDTO(List.of(), List.of("Snippet not found"));
        }
        EngineAdapter adapter = createAdapter(language);
        return adapter.execute(code, version, inputs, envs);
    }

    public Result<UUID> format(UUID snippetId,UUID formatId, Version version, FormatterSupportedRules rules,
            SupportedLanguage language) {
        InputStream src = loadSnippet(snippetId);
        if (src == null) {
            return new IncorrectResult<>("Snippet not found");
        }
        EngineAdapter adapter = createAdapter(language);
        Result<String> formattedResult = adapter.format(src, rules, version);
        if (!formattedResult.isCorrect()) {
            return new IncorrectResult<>("Failed to format");
        }
        return saveSnippet(snippetId,formatId,formattedResult.result());
    }

    public Result<String> analyze(UUID snippetId, Version version, LintSupportedRules rules,
            SupportedLanguage language) {
        InputStream src = loadSnippet(snippetId);
        if (src == null) {
            return new IncorrectResult<>("Snippet not found");
        }

        EngineAdapter adapter = createAdapter(language);
        return adapter.analyze(src, rules, version);
    }

    public Result<List<String>> validate(UUID snippetId, SupportedLanguage language, Version version) {
        RunSnippetResponseDTO exec = execute(language, snippetId, version, List.of(), Map.of());
        if (exec.errors().isEmpty()) {
            logger.info("Snippet validated successfully");
            return new CorrectResult<>(List.of());
        }
        logger.info("Invalid snippet:\n" + String.join("\n", exec.errors()));
        return new IncorrectResult<>("Invalid snippet:\n" + String.join("\n", exec.errors()));
    }

    public TestResponseDTO test(TestRequestDTO dto) {
        try {
            String code = assetService.getSnippet(dto.snippetId()).getBody();
            if (code == null) {
                return new TestResponseDTO(List.of(), List.of("Snippet not found"), SnippetTestStatus.FAILED);
            }

            EngineAdapter adapter = createAdapter(dto.language());
            Version parsedVersion = Version.fromString(dto.version());

            RunSnippetResponseDTO execution = adapter.execute(code, parsedVersion, dto.inputs(), dto.envs());
            List<String> actual = normalize(execution.outputs());
            List<String> expected = normalize(dto.outputs());
            logger.info("{}", execution.errors());
            if (!execution.errors().isEmpty()) {
                return new TestResponseDTO(execution.outputs(), execution.errors(), SnippetTestStatus.FAILED);
            }
            logger.info("{} {}", actual, expected);
            if (!actual.equals(expected)) {
                return new TestResponseDTO(
                        execution.outputs(),
                        List.of("Output mismatch"),
                        SnippetTestStatus.FAILED
                );
            }
            logger.info("{} {} {}", dto.inputs(), dto.outputs(), execution.outputs());
            return new TestResponseDTO(execution.outputs(), List.of(), SnippetTestStatus.PASSED);

        } catch (Exception ex) {
            return new TestResponseDTO(List.of(), List.of("Internal test error: " + ex.getMessage()),
                    SnippetTestStatus.FAILED);
        }
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

    private Result<UUID> saveSnippet(UUID snippetId,UUID formatId,String newContent) {
        try {
            assetService.saveOriginalSnippet(snippetId,formatId);
            return new CorrectResult<>(assetService.saveSnippet(snippetId,newContent).getBody());
        } catch (Exception e) {
            return new IncorrectResult<>("Failed to save snippet");
        }
    }

    private List<String> normalize(List<String> outputs) {
        return outputs.stream()
                .map(s -> s.replace("\r\n", "\n"))
                .map(String::trim)
                .toList();
    }
}
