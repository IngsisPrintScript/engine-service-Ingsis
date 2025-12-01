package com.ingsis.snippetManager.engine;

import com.ingsis.snippetManager.engine.dto.RunSnippetRequestDTO;
import com.ingsis.snippetManager.engine.dto.RunSnippetResponseDTO;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/run")
public class RunController {

    private final PrintScriptRunnerService service;

    public RunController(PrintScriptRunnerService service) {
        this.service = service;
    }

    @PostMapping("/{userId}")
    public RunSnippetResponseDTO run(@PathVariable UUID userId, @RequestBody RunSnippetRequestDTO dto) {
        return new RunSnippetResponseDTO("a", "a", 1);
    }
}