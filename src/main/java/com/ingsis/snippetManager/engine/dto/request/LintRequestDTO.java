package com.ingsis.snippetManager.engine.dto.request;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import java.util.UUID;

public record LintRequestDTO(UUID snippetId, SupportedLanguage language, String version, LintSupportedRules rules) {
}
