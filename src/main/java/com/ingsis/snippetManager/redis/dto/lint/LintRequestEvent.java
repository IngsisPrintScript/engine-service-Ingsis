package com.ingsis.snippetManager.redis.dto.lint;

import com.ingsis.snippetManager.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.engine.supportedRules.LintSupportedRules;
import java.util.UUID;

public record LintRequestEvent(String ownerId, UUID snippetId, SupportedLanguage language,
        LintSupportedRules supportedRules, String version) {
}
