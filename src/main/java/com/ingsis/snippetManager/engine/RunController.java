package com.ingsis.snippetManager.engine;

import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.request.FormatRequestDTO;
import com.ingsis.snippetManager.engine.dto.request.LintRequestDTO;
import com.ingsis.snippetManager.engine.dto.request.RunSnippetRequestDTO;
import com.ingsis.snippetManager.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.engine.dto.response.TestResponseDTO;
import com.ingsis.snippetManager.engine.dto.response.ValidationResult;
import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;
import com.ingsis.utils.result.Result;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/run")
public class RunController {

    private final SnippetRunnerService service;

    public RunController(SnippetRunnerService service) {
        this.service = service;
    }

    @PostMapping("/execute")
    public RunSnippetResponseDTO execute(@AuthenticationPrincipal Jwt jwt, @RequestBody RunSnippetRequestDTO dto) {
        return service.execute(dto.language(), dto.snippetId(), Version.fromString(dto.version()));
    }

    @PostMapping("/format")
    public String format(@AuthenticationPrincipal Jwt jwt, @RequestBody FormatRequestDTO dto) {
        return service.format(dto.snippetId(), Version.fromString(dto.version()), dto.formatterSupportedRules(),
                dto.language()).result();
    }

    @PostMapping("/analyze")
    public ValidationResult analyze(@AuthenticationPrincipal Jwt jwt, @RequestBody LintRequestDTO dto) {
        Result<String> message = service.analyze(dto.snippetId(), Version.fromString(dto.version()), dto.rules(),
                dto.language());
        return new ValidationResult(message.result(), message.isCorrect());
    }
    @PostMapping("/validate")
    public ValidationResult validate(@AuthenticationPrincipal Jwt jwt, @RequestBody LintRequestDTO dto) {
        Result<List<String>> result = service.validate(dto.snippetId(),dto.language(),Version.fromString(dto.version()));
        return new ValidationResult(result.error(), result.isCorrect());
    }
    @PostMapping("/test")
    public TestResponseDTO test(@AuthenticationPrincipal Jwt jwt, @RequestBody TestRequestDTO dto) {
        Result<RunSnippetResponseDTO> tested = service.test(dto);

        RunSnippetResponseDTO response;

        if (tested.isCorrect()) {
            response = tested.result();
        } else {
            response = tested.result() != null
                    ? tested.result()
                    : new RunSnippetResponseDTO(List.of(), List.of("Test execution failed"));
        }
        return new TestResponseDTO(response.outputs(), response.errors(),
                tested.isCorrect() ? SnippetTestStatus.PASSED : SnippetTestStatus.FAILED);
    }
}