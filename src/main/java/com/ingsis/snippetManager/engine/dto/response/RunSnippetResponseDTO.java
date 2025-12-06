package com.ingsis.snippetManager.engine.dto.response;

import java.util.List;

public record RunSnippetResponseDTO(List<String> outputs, List<String> errors) {
}
