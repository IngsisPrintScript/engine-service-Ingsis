package com.ingsis.snippetManager.engine.dto.response;

import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;

import java.util.List;

public record TestResponseDTO(List<String> outputs, List<String> errors, SnippetTestStatus status) {
}
