package com.ingsis.snippetManager.engine.dto.request;

import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;

import java.util.List;
import java.util.UUID;

public record RunSnippetRequestDTO(UUID snippetId, SupportedLanguage language, List<String> inputs, String version){
}
