package com.ingsis.snippetManager.redis.dto;

import java.util.List;
import java.util.UUID;

public record TestReturnDTO(UUID testId, SnippetTestStatus status, List<String> outputs) {
}
