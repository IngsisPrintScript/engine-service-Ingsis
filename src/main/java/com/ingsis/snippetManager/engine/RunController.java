package com.ingsis.snippetManager.engine;

import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.request.FormatRequestDTO;
import com.ingsis.snippetManager.engine.dto.request.LintRequestDTO;
import com.ingsis.snippetManager.engine.dto.request.RunSnippetRequestDTO;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;

import org.springframework.http.ResponseEntity;
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
    public RunSnippetResponseDTO execute(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RunSnippetRequestDTO dto
    ) {
        return service.execute(
                dto.language(),
                dto.snippetId(),
                Version.fromString(dto.version())
        );
    }

    @PostMapping("/format")
    public String format(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FormatRequestDTO dto
    ) {
        return service.format(
                dto.snippetId(),
                Version.fromString(dto.version()),
                dto.formatterSupportedRules(),
                dto.language()
        );
    }

    @PostMapping("/analyze")
    public String analyze(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody LintRequestDTO dto
    ) {
        return service.analyze(
                dto.snippetId(),
                Version.fromString(dto.version()),
                dto.rules(),
                dto.language()
        );
    }
}