package com.ingsis.snippetManager.engine.dto;

public record RunSnippetResponseDTO(String output, String error, int exitCode) {
}
