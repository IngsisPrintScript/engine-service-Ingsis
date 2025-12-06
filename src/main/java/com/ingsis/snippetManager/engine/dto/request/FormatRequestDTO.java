package com.ingsis.snippetManager.engine.dto.request;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.FormatterSupportedRules;
import java.util.UUID;

public record FormatRequestDTO(UUID snippetId, String version, SupportedLanguage language,
        FormatterSupportedRules formatterSupportedRules) {
}
