package com.ingsis.snippetManager.engine.dto.request;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TestRequestDTO(UUID snippetId, List<String> inputs, List<String> outputs, SupportedLanguage language,
        String version, Map<String, String> envs) {
}
