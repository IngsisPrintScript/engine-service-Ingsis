package com.ingsis.snippetManager.engine;

import com.ingsis.snippetManager.engine.dto.RunSnippetRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/run")
public class EngineController {

    @PostMapping("/snippet")
    public ResponseEntity<RunSnippetRequestDTO> snippet(@AuthenticationPrincipal Jwt jwt, @RequestBody RunSnippetRequestDTO snippetDTO) {
        return new ResponseEntity<>(snippetDTO, HttpStatus.OK);
    }
}
